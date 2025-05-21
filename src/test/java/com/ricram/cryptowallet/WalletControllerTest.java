package com.ricram.cryptowallet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ricram.cryptowallet.controller.WalletController;
import com.ricram.cryptowallet.dto.CreateWalletRequest;
import com.ricram.cryptowallet.dto.WalletResponseDto;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WalletController.class)
public class WalletControllerTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private WalletService walletService;

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


}

