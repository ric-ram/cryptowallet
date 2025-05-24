package com.ricram.cryptowallet;


import com.ricram.cryptowallet.dto.AssetInfo;
import com.ricram.cryptowallet.dto.CoinCapAsset;
import com.ricram.cryptowallet.dto.CoinCapAssetHistory;
import com.ricram.cryptowallet.dto.CoinCapHistoryResponseDto;
import com.ricram.cryptowallet.service.impl.CoinCapServiceImpl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
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
    @DisplayName("fetchAssetBySlug() -> 404 when slug is not found")
    void whenAssetSlugDoesNotExist() throws Exception {
        // simulate an empty data array
        server.enqueue(new MockResponse()
                .setBody("{\"data\":{}}")
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

    @Test
    @DisplayName("fetchAssetHistoryBySlug() -> Success when slug exists and so does price")
    void whenFetchAssetHistoryBySlugSuccess() throws Exception {
        String json = """
                {
                    "data": [
                        {"priceUsd": "10000.0", "time": "1747699200000", "date": "2025-05-20T00:00:00.000Z"},
                        {"priceUsd": "12000.0", "time": "1747785600000", "date": "2025-05-21T00:00:00.000Z"}
                    ]
                }
            """;

        server.enqueue(new MockResponse()
                .setBody(json)
                .addHeader("Content-Type", "application/json"));

        Optional<CoinCapHistoryResponseDto> opt = service.fetchAssetHistoryBySlug(
                "bitcoin",
                1747699200000L,
                1747785600000L
        );

        assertTrue(opt.isPresent());
        CoinCapHistoryResponseDto dto = opt.get();
        CoinCapAssetHistory h1 = dto.data().get(0);
        CoinCapAssetHistory h2 = dto.data().get(1);

        assertEquals(h1.priceUsd(), new BigDecimal("10000.0"));
        assertEquals(h1.time(), 1747699200000L);
        assertEquals(h1.date(), Instant.parse("2025-05-20T00:00:00.000Z"));

        assertEquals(h2.priceUsd(), new BigDecimal("12000.0"));
        assertEquals(h2.time(), 1747785600000L);
        assertEquals(h2.date(), Instant.parse("2025-05-21T00:00:00.000Z"));

    }

    @Test
    @DisplayName("fetchAssetHistoryBySlug() -> 404 when slug is not found")
    void whenFetchAssetHistoryBySlugNotFound() throws Exception {
        server.enqueue(new MockResponse()
                .setBody("{\"data\":[]}")
                .addHeader("Content-Type", "application/json"));

        assertTrue(service.fetchAssetHistoryBySlug(
                "noslug",
                1747699200000L,
                1747785600000L
        ).isEmpty());
    }

    @Test
    @DisplayName("fetchAssetHistoryBySlug() -> 503 when there is an issue with CoinCap api")
    void whenFetchAssetHistoryBySlugServerError() throws Exception {

        server.enqueue(new MockResponse().setResponseCode(503));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.fetchAssetHistoryBySlug(
                        "noslug",
                        1747699200000L,
                        1747785600000L
                )
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
    }

}
