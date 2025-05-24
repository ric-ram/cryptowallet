package com.ricram.cryptowallet.service.impl;

import com.ricram.cryptowallet.dao.AssetQuantity;
import com.ricram.cryptowallet.dao.AssetRepository;
import com.ricram.cryptowallet.dao.LatestPriceRepository;
import com.ricram.cryptowallet.dao.WalletRepository;
import com.ricram.cryptowallet.dto.*;
import com.ricram.cryptowallet.entity.LatestPrice;
import com.ricram.cryptowallet.entity.Wallet;
import com.ricram.cryptowallet.service.CoinCapService;
import com.ricram.cryptowallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static java.lang.Long.sum;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;

    private final AssetRepository assetRepository;

    private final LatestPriceRepository latestPriceRepository;

    private final CoinCapService coinCapService;

    @Override
    @Transactional
    public WalletResponseDto create(CreateWalletRequest request) {
        if (walletRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email already in use: " + request.email()
            );
        }

        Wallet newWallet = Wallet.builder()
                .email(request.email())
                .build();

        Wallet savedWallet = walletRepository.save(newWallet);
        return new WalletResponseDto(savedWallet.getId(), savedWallet.getEmail());
    }

    @Override
    @Transactional(readOnly = true)
    public WalletValuationResponseDto getValuation(Long walletId) {

        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Wallet not found"
                ));

        List<AssetQuantity> assetQuantities = assetRepository.findQuantitiesByWalletId(walletId);

        List<AssetValue> assetValues = assetQuantities.stream()
                .map(asset -> {
                    String slug = asset.getSlug();
                    String symbol = asset.getSymbol();
                    double quantity = asset.getTotalQuantity();

                    LatestPrice latestPrice = latestPriceRepository.findBySlug(slug)
                            .orElseThrow(() -> new ResponseStatusException(
                                    HttpStatus.SERVICE_UNAVAILABLE,
                                    "No recorded price for " + slug
                            ));

                    BigDecimal price = latestPrice.getPrice().setScale(2, RoundingMode.HALF_UP);
                    BigDecimal value = price.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);

                    return new AssetValue(symbol, quantity, price, value);
                })
                .toList();

        BigDecimal total = assetValues.stream()
                .map(AssetValue::value)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        return new WalletValuationResponseDto(walletId, total, assetValues);
    }

    @Override
    public WalletSimulationResponseDto simulate(WalletSimulationRequest req) {
        // see if date exists, if not assigned it to today
        LocalDate requestDate = Optional.ofNullable(req.date())
                .orElse(LocalDate.now(ZoneId.of("Europe/Lisbon")));
        boolean isToday = requestDate.isEqual(LocalDate.now(ZoneId.of("Europe/Lisbon")));


        List<AssetProfitSimulation> assetSimulationList =  req.assets().stream()
                .map(a -> {
                    // get asset slugs
                    String slug = coinCapService.fetchAssetBySymbol(a.symbol())
                            .map(AssetInfo::slug)
                            .orElseThrow(() ->
                                    new ResponseStatusException(
                                            HttpStatus.BAD_REQUEST,
                                            "Unknown asset symbol: " + a.symbol()
                                    ));

                    // get prices at the date.
                    // If today get from latest_price table if it exists in table, else get latest price for it
                    // If not today get history for provided date
                    BigDecimal priceAt;
                    if(isToday) {
                        Optional<LatestPrice> cached = latestPriceRepository.findBySlug(slug);
                        if (cached.isPresent()) {
                            priceAt = cached.get().getPrice();
                        } else {
                            AssetInfo info = coinCapService.fetchAssetBySlug(slug)
                                    .orElseThrow(() ->
                                            new ResponseStatusException(
                                                    HttpStatus.BAD_REQUEST,
                                                    "Unknow asset: " + slug
                                            ));

                            LatestPrice lp = LatestPrice.builder()
                                    .slug(info.slug())
                                    .symbol(info.symbol())
                                    .price(info.price())
                                    .fetchedAt(Instant.now())
                                    .build();
                            latestPriceRepository.save(lp);

                            priceAt = lp.getPrice();
                        }
                    } else {
                        CoinCapHistoryResponseDto history = coinCapService.fetchAssetHistoryBySlug(
                                slug,
                                toEpochMillis(requestDate),
                                toEpochMillis(requestDate))
                                .orElseThrow(() ->
                                        new ResponseStatusException(
                                                HttpStatus.BAD_REQUEST,
                                                "No history for " + slug
                                        ));

                        priceAt = history.data().get(0).priceUsd();
                    }

                    // Calculate profits
                    BigDecimal assetValue = priceAt.multiply(BigDecimal.valueOf(a.quantity()));
                    BigDecimal profitAmount = assetValue.subtract(a.value());
                    BigDecimal profitPercentage = profitAmount
                            .divide(a.value(), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100));

                    return new AssetProfitSimulation(
                            a.symbol(),
                            a.quantity(),
                            assetValue,
                            profitAmount,
                            profitPercentage
                    );
                })
                .toList();

        // Define best and worst performer
        BigDecimal total = assetSimulationList.stream()
                .map(AssetProfitSimulation::value)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        AssetProfitSimulation bestAsset = assetSimulationList.stream()
                .max(Comparator.comparing(AssetProfitSimulation::profitPercent))
                .orElseThrow(() -> new NoSuchElementException("No assets to compare"));

        AssetProfitSimulation worstAsset = assetSimulationList.stream()
                .min(Comparator.comparing(AssetProfitSimulation::profitPercent))
                .orElseThrow(() -> new NoSuchElementException("No assets to compare"));

        // Build & return response object
        return new WalletSimulationResponseDto(
                total,
                bestAsset.symbol(),
                bestAsset.profitPercent().setScale(2, RoundingMode.HALF_UP),
                worstAsset.symbol(),
                worstAsset.profitPercent().setScale(2, RoundingMode.HALF_UP)
        );
    }

    public long toEpochMillis(LocalDate date) {
        Instant instant = date
                .atStartOfDay(ZoneId.of("Europe/Lisbon"))   // yields a ZonedDateTime
                .toInstant();                                // converts to an Instant (UTC)

        return instant.toEpochMilli();
    }
}
