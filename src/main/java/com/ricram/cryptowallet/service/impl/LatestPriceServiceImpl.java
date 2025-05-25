package com.ricram.cryptowallet.service.impl;

import com.ricram.cryptowallet.dao.AssetRepository;
import com.ricram.cryptowallet.dao.LatestPriceRepository;
import com.ricram.cryptowallet.entity.LatestPrice;
import com.ricram.cryptowallet.service.CoinCapService;
import com.ricram.cryptowallet.service.LatestPriceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class LatestPriceServiceImpl implements LatestPriceService {

    private final AssetRepository assetRepository;

    private final LatestPriceRepository latestPriceRepository;

    private final CoinCapService coinCapService;

    private final ThreadPoolTaskExecutor priceUpdateExecutor;

    /**
     * Manually trigger a full refresh of all the current prices of the saved assets.
     */
    @Override
    public void fetchAndStoreLatestPrice() {

        List<String> slugs = assetRepository.findDistinctSlugs();
        System.out.println(slugs.toString());
        CompletableFuture<?>[] futures = slugs.stream()
                .map(slug ->
                        CompletableFuture.runAsync(() -> updatePriceForSlug(slug), priceUpdateExecutor)
                                .exceptionally(ex -> {
                                    log.error("Price update failed of slug {}: {}", slug, ex.getMessage());
                                    return null;
                                })
                )
                .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(futures).join();
    }

    /**
     * Upserts an asset price to its latest price.
     *
     * @param slug of the asset to update the price to its latest.
     */
    private void updatePriceForSlug(String slug) {

        try {
            coinCapService.fetchAssetBySlug(slug)
                    .ifPresent(info -> {
                        LatestPrice latestPrice = latestPriceRepository.findBySlug(slug)
                                .map(existing -> {
                                    existing.setPrice(info.price());
                                    existing.setFetchedAt(Instant.now());
                                    return existing;
                                })
                                .orElseGet(() -> LatestPrice.builder()
                                        .slug(slug)
                                        .symbol(info.symbol())
                                        .price(info.price())
                                        .fetchedAt(Instant.now())
                                        .build());
                        latestPriceRepository.save(latestPrice);
                        log.debug("Upserted price {} for slug {}", info.price(), slug);
                    });
        } catch (ResponseStatusException e) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Failed to fetch price for slug " + slug,
                    e
            );
        }
    }

    /**
     * Scheduled runner -> Fires every poll-rate-ms milliseconds.
     * Configured in application.properties under app.prices.poll-rate-ms
     */
    @Scheduled(fixedDelayString = "${app.prices.poll-rate-ms:300000}")
    public void scheduledFetchAndStore() {
        log.info("Starting scheduled price update for all distinct slugs");
        fetchAndStoreLatestPrice();
        log.info("Completed scheduled price update");
    }

}
