package com.ricram.cryptowallet;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.*;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "coincap.api.key=dummy-key",
                "app.prices.poll-rate-ms=3600000"
        }
)
@EnableWireMock
@TestPropertySource(properties = {
        "coincap.api.base-url=${wiremock.server.baseUrl}"
})
@ActiveProfiles("test")
@Testcontainers
@Slf4j
public class AppIntegrationTest {

        @Container
        static PostgreSQLContainer<?> pg =
                new PostgreSQLContainer<>("postgres:16-alpine")
                        .withDatabaseName("testdb")
                        .withUsername("test")
                        .withPassword("test");


        @Autowired
        private TestRestTemplate rest;

        @InjectWireMock
        private WireMockServer wireMockServer;

        @DynamicPropertySource
        static void dbProps(DynamicPropertyRegistry reg) {
                reg.add("spring.datasource.url", pg::getJdbcUrl);
                reg.add("spring.datasource.username", pg::getUsername);
                reg.add("spring.datasource.password", pg::getPassword);
        }

        @BeforeAll
        static void checkDocker() {
                Assumptions.assumeTrue(
                        DockerClientFactory.instance().isDockerAvailable(),
                        "Skipping integration tests because Docker is not available"
                );
        }

        @BeforeEach
        void stubCoinCap() {
                WireMock.configureFor(wireMockServer.port());

                wireMockServer.resetAll();

                // Search by Symbol stub
                stubFor(get(urlPathEqualTo("/v3/assets"))
                        .withQueryParam("search", equalTo("BTC"))
                        .withQueryParam("limit", equalTo("1"))
                        .willReturn(okJson("""
                                { "data":[{"id":"bitcoin","symbol":"BTC","priceUsd":"60000.00"}] }
                        """))
                );

                // Slug lookup stub
                stubFor(get(urlPathEqualTo("/v3/assets/bitcoin"))
                        .willReturn(okJson("""
                                { "data":{ "id":"bitcoin","symbol":"BTC","priceUsd":"60000.00" } }
                        """))
                );

                // History on 2025-01-07
                long millis = Instant.parse("2025-01-07T00:00:00.000Z").toEpochMilli();
                stubFor(get(urlPathEqualTo("/v3/assets/bitcoin/history"))
                        .willReturn(okJson(String.format("""
                                { "data":[
                                           { "priceUsd":"50000.00", "time": %d,
                                             "date":"2025-01-07T00:00:00.000Z" }
                                         ]}
                        """, millis)))
                );
        }

        @Test
        @DisplayName("Testing a full workflow of Creating wallet, Adding an asset to that wallet " +
                "and Getting that wallet valuation")
        void fullWorkflowCreateAddValuate() {

                // Create a wallet
                log.info("STEP 1: Creating a new wallet for test@example.com");
                ResponseEntity<Map> c = rest.postForEntity(
                        "/wallet",
                        Map.of("email", "test@example.com"),
                        Map.class
                );
                assertThat(c.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                Integer walletId = (Integer) c.getBody().get("id");

                // Add asset to created wallet
                log.info("STEP 2: Adding 2.0 BTC to wallet {}", walletId);
                ResponseEntity<Void> add = rest.postForEntity(
                        "/wallet/{id}/asset",
                        Map.of("symbol", "BTC", "quantity", 2.0, "price", new BigDecimal("10000.0")),
                        Void.class,
                        walletId
                );
                assertThat(add.getStatusCode()).isEqualTo(HttpStatus.CREATED);

                // Get wallet Valuation.
                log.info("STEP 3: Fetching valuation for wallet {}", walletId);
                ResponseEntity<Map> val = rest.getForEntity(
                        "/wallet/{id}",
                        Map.class,
                        walletId
                );
                assertThat(val.getStatusCode()).isEqualTo(HttpStatus.OK);
                BigDecimal total = new BigDecimal(val.getBody().get("total").toString());
                assertThat(total).isEqualByComparingTo("120000.00");
        }

        @Test
        @DisplayName("Simulating the Profit/Loss of a given wallet")
        void walletSimulationWithPastDate() {

                // Simulate
                log.info("STEP 1: Simulating wallet profit/loss");
                Map<String,Object> req = Map.of(
                        "date","07/01/2025",
                        "assets", new Object[]{ Map.of(
                                "symbol","BTC","quantity",2.0,
                                "value",new BigDecimal("50000.00")
                        )}
                );
                ResponseEntity<Map> resp = rest.postForEntity(
                        "/wallet/simulate",
                        req,
                        Map.class
                );
                assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
                BigDecimal profit = new BigDecimal(resp.getBody().get("total").toString());
                assertThat(profit).isEqualByComparingTo("100000.00");

        }
}
