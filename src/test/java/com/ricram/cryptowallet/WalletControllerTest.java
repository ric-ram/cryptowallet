package com.ricram.cryptowallet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ricram.cryptowallet.controller.WalletController;
import com.ricram.cryptowallet.dao.AssetQuantity;
import com.ricram.cryptowallet.dto.*;
import com.ricram.cryptowallet.service.AssetService;
import com.ricram.cryptowallet.service.WalletService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import javax.print.attribute.standard.Media;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WalletController.class)
public class WalletControllerTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private WalletService walletService;

    @MockitoBean
    private AssetService assetService;

    @Test
    @DisplayName("POST /wallet -> 400 when email is missing")
    void whenEmailMissing() throws Exception {
        mvc.perform(post("/wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /wallet -> 400 when email present but blank")
    void whenEmailPresentButBlank() throws Exception {
        mvc.perform(post("/wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"email\": \"\" }"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /wallet -> 400 when email format is invalid")
    void whenEmailIsBadFormat() throws Exception {
        mvc.perform(post("/wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"email\": \"not-an-email\" }"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /wallet -> 400 when email is null")
    void whenEmailIsNull() throws Exception {
        // payload with explicit null value
        mvc.perform(post("/wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"email\": null }"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /wallet -> 201 Created with location and body when email is valid")
    void whenEmailIsValid() throws Exception {

        // arrange
        WalletResponseDto dto = new WalletResponseDto(123L, "test@example.com");
        when(walletService.create(any(CreateWalletRequest.class))).thenReturn(dto);

        String body = objectMapper.writeValueAsString(new CreateWalletRequest("test@example.com"));


        // act & assert
        mvc.perform(post("/wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/wallet/123"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(123))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    @DisplayName("POST /wallet -> 409 Conflict when email already exists")
    void whenEmailIsDuplicate() throws Exception {

        // arrange: service throws 409
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use: test@example.com"))
                .when(walletService).create(any(CreateWalletRequest.class));

        String body = objectMapper.writeValueAsString(new CreateWalletRequest("test@example.com"));

        // act & assert
        mvc.perform(post("/wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /wallet/{id}/asset -> 404 if wallet not found")
    void whenAssetWithInvalidWalletId() throws Exception {

        long invalidWalletId = 99L;
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"))
                .when(assetService).addAsset(eq(invalidWalletId), any(AddAssetRequest.class));

        String body = objectMapper.writeValueAsString(new AddAssetRequest("ETH", new BigDecimal("100.0"), 2.0));

        mvc.perform(post("/wallet/{id}/asset", invalidWalletId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /wallet/{id}/asset -> 201 created with location and body")
    void whenAssetWithValidWalletId() throws Exception {
        long id = 1L;
        BigDecimal price = new BigDecimal("1000.0");
        AssetResponseDto dto = new AssetResponseDto(5L, "ETH", price, 5.0);
        when(assetService.addAsset(eq(id), any(AddAssetRequest.class)))
                .thenReturn(dto);

        String body = objectMapper.writeValueAsString(new AddAssetRequest("ETH", price, 5.0));

        mvc.perform(post("/wallet/{id}/asset", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/wallet/1/asset/5"))
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.symbol").value("ETH"))
                .andExpect(jsonPath("$.price").value(1000.0))
                .andExpect(jsonPath("$.quantity").value(5.0));
    }

    @Test
    @DisplayName("GET /wallet/{id} -> 404 if wallet not found")
    void whenWalletWithInvalidId() throws Exception {

        Long id = 44L;
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"))
                .when(walletService).getValuation(id);

        mvc.perform(get("/wallet/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /wallet/{id} -> 503 if a price can't be found")
    void whenPriceNotFound() throws Exception {

        Long id = 1L;
        doThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No recorded price for BTC"))
                .when(walletService).getValuation(id);

        mvc.perform(get("/wallet/{id}", id))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    @DisplayName("GET /wallet/{id} -> 200 return the total valuation of the wallet and of each asset")
    void whenGetValuationSuccess() throws Exception{

        Long id = 1L;
        AssetValue a1 = new AssetValue("BTC", 4.0,
                new BigDecimal("1000.0"), new BigDecimal("4000.0"));
        AssetValue a2 = new AssetValue("ETH", 5.0,
                new BigDecimal("500.0"), new BigDecimal("2500.0"));
        WalletValuationResponseDto dto = new WalletValuationResponseDto(
                id,
                new BigDecimal("6500.0"),
                List.of(a1, a2)
        );
        when(walletService.getValuation(id))
                .thenReturn(dto);

        mvc.perform(get("/wallet/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.total").value(6500))
                .andExpect(jsonPath("$.assets[0].symbol").value("BTC"))
                .andExpect(jsonPath("$.assets[0].quantity").value(4.0))
                .andExpect(jsonPath("$.assets[0].price").value(1000.0))
                .andExpect(jsonPath("$.assets[0].value").value(4000.0))
                .andExpect(jsonPath("$.assets[1].symbol").value("ETH"))
                .andExpect(jsonPath("$.assets[1].quantity").value(5.0))
                .andExpect(jsonPath("$.assets[1].price").value(500.0))
                .andExpect(jsonPath("$.assets[1].value").value(2500.0));
    }

    @Test
    @DisplayName("POST /wallet/simulate → 400 when bad request")
    void whenSimulateWalletWithInvalidPayload() throws Exception {
        WalletSimulationRequest reqDto = new WalletSimulationRequest(
                LocalDate.of(2025,5,1),
                List.of(new AssetSimulation("FOO", 1.0, new BigDecimal("1000.00")))
        );
        String json = objectMapper.writeValueAsString(reqDto);

        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid payload"))
                .when(walletService).simulateWallet(any(WalletSimulationRequest.class));

        mvc.perform(post("/wallet/simulate")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /wallet/simulate → 503 when service unavailable")
    void whenSimulateWalletServiceUnavailable() throws Exception {
        WalletSimulationRequest reqDto = new WalletSimulationRequest(
                LocalDate.of(2025,5,1),
                List.of(new AssetSimulation("BTC", 2.0, new BigDecimal("1000.00")))
        );
        String json = objectMapper.writeValueAsString(reqDto);

        doThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Dependency down"))
                .when(walletService).simulateWallet(any(WalletSimulationRequest.class));

        mvc.perform(post("/wallet/simulate")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    @DisplayName("POST /wallet/simulate → 200 with correct body on success")
    void whenSimulateWalletSuccess() throws Exception {
        WalletSimulationRequest reqDto = new WalletSimulationRequest(
                LocalDate.of(2025,5,1),
                List.of(
                        new AssetSimulation("BTC", 2.0, new BigDecimal("1000.00")),
                        new AssetSimulation("ETH", 4.0, new BigDecimal("800.00"))
                )
        );
        String jsonReq = objectMapper.writeValueAsString(reqDto);

        // Suppose profit = 2.00, performance = 20.00
        var respDto = new WalletSimulationResponseDto(
                new BigDecimal("2500.00"),
                "BTC",
                new BigDecimal("50.00"),
                "ETH",
                new BigDecimal("25.00")
        );
        when(walletService.simulateWallet(any(WalletSimulationRequest.class)))
                .thenReturn(respDto);

        mvc.perform(post("/wallet/simulate")
                        .contentType("application/json")
                        .content(jsonReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2500.00))
                .andExpect(jsonPath("$.best_asset").value("BTC"))
                .andExpect(jsonPath("$.best_performance").value(50.00))
                .andExpect(jsonPath("$.worst_asset").value("ETH"))
                .andExpect(jsonPath("$.worst_performance").value(25.00));
    }
}

