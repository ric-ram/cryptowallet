package com.ricram.cryptowallet;

import com.ricram.cryptowallet.dao.AssetRepository;
import com.ricram.cryptowallet.dao.LatestPriceRepository;
import com.ricram.cryptowallet.dto.AssetInfo;
import com.ricram.cryptowallet.entity.Asset;
import com.ricram.cryptowallet.entity.LatestPrice;
import com.ricram.cryptowallet.service.CoinCapService;
import com.ricram.cryptowallet.service.LatestPriceService;
import com.ricram.cryptowallet.service.impl.LatestPriceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.*;

public class LatestPriceServiceImplTest {

    private AssetRepository assetRepository;

    private LatestPriceRepository latestPriceRepository;

    private CoinCapService coinCapService;

    private ThreadPoolTaskExecutor executor;

    private LatestPriceService service;

    @BeforeEach
    void setup() throws IOException {
        assetRepository = mock(AssetRepository.class);
        latestPriceRepository = mock(LatestPriceRepository.class);
        coinCapService = mock(CoinCapService.class);

        executor = new ThreadPoolTaskExecutor(){
            @Override
            public void execute(Runnable task) {
                task.run();
            }
        };
        executor.initialize();

        service = new LatestPriceServiceImpl(
                assetRepository,
                latestPriceRepository,
                coinCapService,
                executor
        );
    }

    @Test
    @DisplayName("fetchAndStoreLatestPrice() -> Upserts current prices for distinct slugs")
    void whenFetchAndStoreLatestPriceSuccess() {

        Asset a1 = Asset.builder().symbol("BTC").slug("bitcoin").build();
        Asset a2 = Asset.builder().symbol("ETH").slug("ethereum").build();
        Asset a3 = Asset.builder().symbol("btc").slug("bitcoin").build();
        when(assetRepository.findDistinctSlugs()).thenReturn(List.of(a1.getSlug(), a2.getSlug()));

        assertThat(service).isNotNull();
        assertThat(assetRepository.findDistinctSlugs()).hasSize(2);

        when(coinCapService.fetchAssetBySlug("bitcoin"))
                .thenReturn(Optional.of(new AssetInfo("bitcoin", "BTC", new BigDecimal("60000.0"))));
        when(coinCapService.fetchAssetBySlug("ethereum"))
                .thenReturn(Optional.of(new AssetInfo("ethereum", "ETH", new BigDecimal("4000.0"))));

        LatestPrice existingBtc = LatestPrice.builder()
                .id(1L)
                .slug("bitcoin")
                .price(new BigDecimal("68000.0"))
                .fetchedAt(Instant.now())
                .build();
        when(latestPriceRepository.findBySlug("bitcoin"))
                .thenReturn(Optional.of(existingBtc));
        when(latestPriceRepository.findBySlug("ethereum"))
                .thenReturn(Optional.empty());

        ArgumentCaptor<LatestPrice> captor = ArgumentCaptor.forClass(LatestPrice.class);
        when(latestPriceRepository.save(captor.capture()))
                .thenAnswer(inv -> inv.getArgument(0));

        service.fetchAndStoreLatestPrice();

        List<LatestPrice> saved = captor.getAllValues();
        assertThat(saved).hasSize(2);
        System.out.println("Saved " + saved.get(0));

        LatestPrice upBtc = saved.stream()
                .filter(lp -> lp.getSlug().equals("bitcoin"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected BTC upsert not found"));
        assertThat(upBtc.getId()).isEqualTo(1L);
        assertThat(upBtc.getPrice()).isEqualByComparingTo("60000.0");
        assertThat(upBtc.getFetchedAt()).isCloseTo(Instant.now(), within(5, ChronoUnit.SECONDS));

        LatestPrice upEth = saved.stream()
                .filter(lp -> lp.getSlug().equals("ethereum"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected ETH upsert not found"));
        assertThat(upEth.getId()).isNull();  // new row (no id yet)
        assertThat(upEth.getPrice()).isEqualByComparingTo("4000.0");
        assertThat(upEth.getFetchedAt()).isCloseTo(Instant.now(), within(5, ChronoUnit.SECONDS));

        verify(assetRepository, times(2)).findDistinctSlugs();
        verify(coinCapService).fetchAssetBySlug("bitcoin");
        verify(coinCapService).fetchAssetBySlug("ethereum");
        verify(latestPriceRepository, times(2)).save(any());
    }

    @Test
    @DisplayName("fetchAndStoreLatestPrice() -> skips unknown slugs without saving")
    void whenFetchAndStoreLatestPriceFails() {

        Asset a = Asset.builder().symbol("FOO").slug("foobar").build();
        when(assetRepository.findDistinctSlugs()).thenReturn(List.of(a.getSlug()));
        when(coinCapService.fetchAssetBySlug("foobar")).thenReturn(Optional.empty());

        service.fetchAndStoreLatestPrice();

        verify(latestPriceRepository, never()).save(any());
    }
}
