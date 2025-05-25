package com.ricram.cryptowallet;

import com.ricram.cryptowallet.dao.AssetQuantity;
import com.ricram.cryptowallet.dao.AssetRepository;
import com.ricram.cryptowallet.dao.LatestPriceRepository;
import com.ricram.cryptowallet.dao.WalletRepository;
import com.ricram.cryptowallet.dto.*;
import com.ricram.cryptowallet.entity.LatestPrice;
import com.ricram.cryptowallet.entity.Wallet;
import com.ricram.cryptowallet.service.CoinCapService;
import com.ricram.cryptowallet.service.WalletService;
import com.ricram.cryptowallet.service.impl.WalletServiceImpl;
import jakarta.persistence.criteria.CriteriaBuilder;
import org.checkerframework.checker.units.qual.C;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WalletServiceImplTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private LatestPriceRepository latestPriceRepository;

    @Mock
    private CoinCapService coinCapService;

    @InjectMocks
    private WalletServiceImpl walletService;

    private static final ZoneId ZONE = ZoneId.of("Europe/Lisbon");
    private LocalDate today;

    @BeforeEach
    void setUp() {
        today = LocalDate.now(ZONE);
    }

    @Test
    @DisplayName("create() -> Conflict when email already exists")
    void createWithDuplicateEmail() {

        // arrange
        String email = "test@example.com";
        when(walletRepository.existsByEmail(email)).thenReturn(true);
        CreateWalletRequest req = new CreateWalletRequest(email);

        // act & assert
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> walletService.create(req)
        );
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("already in use"));

        verify(walletRepository).existsByEmail(email);
        verifyNoMoreInteractions(walletRepository);
    }

    @Test
    @DisplayName("create() -> Success when email is new, returns correct DTO")
    void createWithNewEmail() {

        // arrange
        String email = "testing@example.com";
        when(walletRepository.existsByEmail(email)).thenReturn(false);

        ArgumentCaptor<Wallet> captor = ArgumentCaptor.forClass(Wallet.class);
        when(walletRepository.save(captor.capture()))
                .thenAnswer(invocation -> {
                    Wallet w = invocation.getArgument(0);
                    w.setId(53L);
                    w.setCreateAt(Instant.parse("2025-05-21T12:00:00Z"));
                    return w;
                });
        CreateWalletRequest req = new CreateWalletRequest(email);

        // act
        WalletResponseDto dto = walletService.create(req);

        // assert
        assertEquals(53L, dto.id());
        assertEquals(email, dto.email());

        Wallet saved = captor.getValue();
        assertEquals(email, dto.email());
        assertNotNull(saved.getCreateAt());

        verify(walletRepository).existsByEmail(email);
        verify(walletRepository).save(any(Wallet.class));
        verifyNoMoreInteractions(walletRepository);

    }

    @Test
    @DisplayName("getValuation() -> 404 when walletId does not exist")
    void getValuationInvalidWalletId() {

        Long invalidId = 99L;
        when(walletRepository.findById(invalidId)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> walletService.getValuation(invalidId)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(ex.getReason().contains("not found"));

        verify(walletRepository).findById(invalidId);
        verifyNoMoreInteractions(walletRepository, assetRepository, latestPriceRepository);
    }

    @Test
    @DisplayName("getValuation() -> 503 when price is not found for an asset")
    void getValuationNoAssetPrice() {

        Wallet wallet = new Wallet(1L, "test@example.com", Instant.now());
        when(walletRepository.findById(wallet.getId())).thenReturn(Optional.of(wallet));

        AssetQuantity qty = new AssetQuantity() {
            @Override
            public String getSlug() {
                return "bitcoin";
            }

            @Override
            public String getSymbol() {
                return "BTC";
            }

            @Override
            public double getTotalQuantity() {
                return 3.0;
            }
        };
        when(assetRepository.findQuantitiesByWalletId(wallet.getId()))
                .thenReturn(List.of(qty));

        when(latestPriceRepository.findBySlug("bitcoin"))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> walletService.getValuation(wallet.getId())
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
        assertTrue(ex.getReason().contains("No recorded price"));

        verify(walletRepository).findById(wallet.getId());
        verify(assetRepository).findQuantitiesByWalletId(wallet.getId());
        verify(latestPriceRepository).findBySlug("bitcoin");
        verifyNoMoreInteractions(walletRepository, assetRepository, latestPriceRepository);
    }

    @Test
    @DisplayName("getValuation() -> Success returning the wallet valuation")
    void getValuationSuccess() {

        Wallet wallet = new Wallet(1L, "test@example.com", Instant.now());
        when(walletRepository.findById(wallet.getId())).thenReturn(Optional.of(wallet));

        AssetQuantity qtyBtc = new AssetQuantity() {
            @Override
            public String getSlug() {
                return "bitcoin";
            }

            @Override
            public String getSymbol() {
                return "BTC";
            }

            @Override
            public double getTotalQuantity() {
                return 4.0;
            }
        };
        AssetQuantity qtyEth = new AssetQuantity() {
            @Override
            public String getSlug() {
                return "ethereum";
            }

            @Override
            public String getSymbol() {
                return "ETH";
            }

            @Override
            public double getTotalQuantity() {
                return 3.0;
            }
        };
        when(assetRepository.findQuantitiesByWalletId(wallet.getId()))
                .thenReturn(List.of(qtyBtc, qtyEth));

        LatestPrice btcPrice = LatestPrice.builder()
                .slug("bitcoin")
                .symbol("BTC")
                .price(new BigDecimal("10000.0"))
                .fetchedAt(Instant.now())
                .build();
        LatestPrice ethPrice = LatestPrice.builder()
                .slug("ethereum")
                .symbol("ETH")
                .price(new BigDecimal("4000.0"))
                .fetchedAt(Instant.now())
                .build();
        when(latestPriceRepository.findBySlug("bitcoin"))
                .thenReturn(Optional.of(btcPrice));
        when(latestPriceRepository.findBySlug("ethereum"))
                .thenReturn(Optional.of(ethPrice));

        WalletValuationResponseDto dto = walletService.getValuation(wallet.getId());

        assertEquals(dto.id(), wallet.getId());

        List<AssetValue> assetValues = dto.assets();
        assertThat(assetValues).hasSize(2);

        AssetValue btc = assetValues.get(0);
        assertEquals(btc.symbol(), "BTC");
        assertEquals(btc.quantity(), 4.0);
        assertThat(btc.price()).isEqualByComparingTo("10000.0");
        assertThat(btc.value()).isEqualByComparingTo("40000.0");

        AssetValue eth = assetValues.get(1);
        assertEquals(eth.symbol(), "ETH");
        assertEquals(eth.quantity(), 3.0);
        assertThat(eth.price()).isEqualByComparingTo("4000.0");
        assertThat(eth.value()).isEqualByComparingTo("12000.0");

        assertThat(dto.total()).isEqualByComparingTo("52000.0");

        verify(walletRepository).findById(wallet.getId());
        verify(assetRepository).findQuantitiesByWalletId(wallet.getId());
        verify(latestPriceRepository).findBySlug("bitcoin");
        verify(latestPriceRepository).findBySlug("ethereum");
        verifyNoMoreInteractions(walletRepository, assetRepository, latestPriceRepository);
    }

    @Test
    @DisplayName("simulateWallet() -> 400 when symbol unknown")
    void simulateWalletUnknownSymbol() {
        AssetSimulation assetReq = new AssetSimulation("FOO", 1.0, new BigDecimal("1000.0"));
        WalletSimulationRequest req = new WalletSimulationRequest(null, List.of(assetReq));

        when(coinCapService.fetchAssetBySymbol("FOO")).thenReturn(Optional.empty());

        var ex = assertThrows(ResponseStatusException.class,
                () -> walletService.simulateWallet(req));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getReason()).contains("Unknown asset symbol: FOO");
    }

    @Test
    @DisplayName("simulateWallet() → today + cached price")
    void simulateWalletTodayCached() {
        AssetSimulation assetReq = new AssetSimulation("BTC", 2.0, new BigDecimal("1000.0"));
        WalletSimulationRequest req = new WalletSimulationRequest(today, List.of(assetReq));

        when(coinCapService.fetchAssetBySymbol("BTC"))
                .thenReturn(Optional.of(new AssetInfo("bitcoin","BTC",BigDecimal.ZERO)));

        var cached = LatestPrice.builder()
                .slug("bitcoin")
                .symbol("BTC")
                .price(new BigDecimal("600.00"))
                .fetchedAt(Instant.now())
                .build();
        when(latestPriceRepository.findBySlug("bitcoin"))
                .thenReturn(Optional.of(cached));

        WalletSimulationResponseDto resp = walletService.simulateWallet(req);

        // compute: assetValue = 600 × 2 = 1200; profit = 1200 − 1000 = 200; perf = 200/1000*100 = 20.00
        assertThat(resp.total()).isEqualByComparingTo("1200.00");
        assertThat(resp.bestAsset()).isEqualTo("BTC");
        assertThat(resp.bestPerformance()).isEqualByComparingTo("20.00");
        assertThat(resp.worstAsset()).isEqualTo("BTC");
        assertThat(resp.worstPerformance()).isEqualByComparingTo("20.00");
    }

    @Test
    @DisplayName("simulateWallet() → today + no cache for slug")
    void simulateWalletTodayNotCached() {
        AssetSimulation assetReq = new AssetSimulation("ETH", 1.5, new BigDecimal("500.0"));
        WalletSimulationRequest req = new WalletSimulationRequest(null, List.of(assetReq));

        when(coinCapService.fetchAssetBySymbol("ETH"))
                .thenReturn(Optional.of(new AssetInfo("ethereum","ETH",BigDecimal.ZERO)));

        when(latestPriceRepository.findBySlug("ethereum"))
                .thenReturn(Optional.empty());

        when(coinCapService.fetchAssetBySlug("ethereum"))
                .thenReturn(Optional.of(new AssetInfo("ethereum","ETH", new BigDecimal("800.0"))));

        ArgumentCaptor<LatestPrice> cap = ArgumentCaptor.forClass(LatestPrice.class);
        when(latestPriceRepository.save(cap.capture()))
                .thenAnswer(inv -> inv.getArgument(0));

        var resp = walletService.simulateWallet(req);

        assertThat(cap.getValue().getPrice()).isEqualByComparingTo("800.0");

        // compute: assetValue = 800 * 1.5 = 1200; profit = 1200 - 500 = 700; perf = 700 / 500 * 100 = 140.00
        assertThat(resp.total()).isEqualByComparingTo("1200.00");
        assertThat(resp.bestAsset()).isEqualTo("ETH");
        assertThat(resp.bestPerformance()).isEqualByComparingTo("140.00");
        assertThat(resp.bestAsset()).isEqualTo("ETH");
        assertThat(resp.bestPerformance()).isEqualByComparingTo("140.00");
    }

    @Test
    @DisplayName("simulateWallet() → past date gets history from CoinCap endpoint")
    void simulateWalletPastDate() {
        LocalDate past = today.minusDays(1);
        AssetSimulation assetReq = new AssetSimulation("BTC", 2.0, new BigDecimal("1000.0"));
        WalletSimulationRequest req = new WalletSimulationRequest(past, List.of(assetReq));

        when(coinCapService.fetchAssetBySymbol("BTC"))
                .thenReturn(Optional.of(new AssetInfo("bitcoin","BTC",BigDecimal.ZERO)));

        var point = new CoinCapAssetHistory(
                new BigDecimal("700.00"),
                past.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
                past.atStartOfDay(ZoneOffset.UTC).toInstant()
        );
        when(coinCapService.fetchAssetHistoryBySlug(eq("bitcoin"), anyLong(), anyLong()))
                .thenReturn(Optional.of(new CoinCapHistoryResponseDto(List.of(point))));

        var resp = walletService.simulateWallet(req);

        // compute: assetValue = priceAt = 700 * 2= 1400; profit = 1400 - 1000 = 400; perf = 400 / 1000 * 100 = 40.00
        assertThat(resp.total()).isEqualByComparingTo("1400.00");
        assertThat(resp.bestAsset()).isEqualTo("BTC");
        assertThat(resp.bestPerformance()).isEqualByComparingTo("40.00");
        assertThat(resp.worstAsset()).isEqualTo("BTC");
        assertThat(resp.worstPerformance()).isEqualByComparingTo("40.00");
    }

    @Test
    @DisplayName("simulateWallet() → 400 when no history for past date")
    void simulate_pastDateNoHistory_throwsBadRequest() {
        LocalDate past = today.minusDays(2);
        AssetSimulation assetReq = new AssetSimulation("BTC", 3.0, new BigDecimal("300.0"));
        WalletSimulationRequest req = new WalletSimulationRequest(past, List.of(assetReq));

        when(coinCapService.fetchAssetBySymbol("BTC"))
                .thenReturn(Optional.of(new AssetInfo("bitcoin","BTC",BigDecimal.ZERO)));

        when(coinCapService.fetchAssetHistoryBySlug(eq("bitcoin"), anyLong(), anyLong()))
                .thenReturn(Optional.empty());

        var ex = assertThrows(ResponseStatusException.class,
                () -> walletService.simulateWallet(req));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getReason()).contains("No history for bitcoin");
    }
}
