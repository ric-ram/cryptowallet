package com.ricram.cryptowallet;


import com.ricram.cryptowallet.dto.AssetInfo;
import com.ricram.cryptowallet.service.impl.CoinCapServiceImpl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class CoinCapServiceImplTest {

    private MockWebServer server;

    private CoinCapServiceImpl service;

    @BeforeEach
    void setup() throws IOException {
        server = new MockWebServer();
        server.start();

        WebClient client = WebClient.builder()
                .baseUrl(server.url("/").toString())
                .build();

        service = new CoinCapServiceImpl(client);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }


    @Test
    @DisplayName("fetchAssetBySymbol() -> Success when the symbol exists")
    void whenAssetSymbolExist()  throws Exception {
        String json = """
                {
                    "data": [
                        { "id": "bitcoin", "symbol": "BTC", "priceUsd": "60000.0" }
                    ]
                }
            """;

        server.enqueue(new MockResponse()
                .setBody(json)
                .addHeader("Content-Type", "application/json"));

        Optional<AssetInfo> opt = service.fetchAssetBySymbol("btc");

        assertTrue(opt.isPresent());
        AssetInfo info = opt.get();
        assertEquals("bitcoin", info.slug());
        assertEquals("BTC", info.symbol());
        assertEquals(new BigDecimal("60000.0"), info.price());
    }

    @Test
    @DisplayName("fetchAssetBySymbol() -> 404 when symbol is not found")
    void whenAssetSymbolDoesNotExist() throws Exception {
        // simulate an empty data array
        server.enqueue(new MockResponse()
                .setBody("{\"data\":[]}")
                .addHeader("Content-Type", "application/json"));

        assertTrue(service.fetchAssetBySymbol("NOSYM").isEmpty());
    }

    @Test
    @DisplayName("fetchAssetBySymbol() -> 503 when there is an issue with CoinCap api")
    void whenFetchBySymbolServerError() throws Exception {

         server.enqueue(new MockResponse().setResponseCode(503));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.fetchAssetBySymbol("btc")
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
    }

    @Test
    @DisplayName("fetchAssetBySlug() -> Success when the symbol exists")
    void whenAssetSlugExist()  throws Exception {
        String json = """
                {
                    "data": {
                        "id": "bitcoin", "symbol": "BTC", "priceUsd": "60000.0"
                    }
                }
            """;

        server.enqueue(new MockResponse()
                .setBody(json)
                .addHeader("Content-Type", "application/json"));

        Optional<AssetInfo> opt = service.fetchAssetBySlug("bitcoin");

        assertTrue(opt.isPresent());
        AssetInfo info = opt.get();
        assertEquals("bitcoin", info.slug());
        assertEquals("BTC", info.symbol());
        assertEquals(new BigDecimal("60000.0"), info.price());
    }

    @Test
    @DisplayName("fetchAssetBySlug() -> 404 when symbol is not found")
    void whenAssetSlugDoesNotExist() throws Exception {
        // simulate an empty data array
        server.enqueue(new MockResponse()
                .setBody("{\"data\":[]}")
                .addHeader("Content-Type", "application/json"));

        assertTrue(service.fetchAssetBySlug("noslug").isEmpty());
    }

    @Test
    @DisplayName("fetchAssetBySlug() -> 503 when there is an issue with CoinCap api")
    void whenFetchBySlugServerError() throws Exception {

        server.enqueue(new MockResponse().setResponseCode(503));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.fetchAssetBySlug("bitcoin")
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
    }

}
