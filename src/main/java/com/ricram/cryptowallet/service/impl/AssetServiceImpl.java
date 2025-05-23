package com.ricram.cryptowallet.service.impl;

import com.ricram.cryptowallet.dao.AssetRepository;
import com.ricram.cryptowallet.dao.LatestPriceRepository;
import com.ricram.cryptowallet.dao.WalletRepository;
import com.ricram.cryptowallet.dto.AddAssetRequest;
import com.ricram.cryptowallet.dto.AssetInfo;
import com.ricram.cryptowallet.dto.AssetResponseDto;
import com.ricram.cryptowallet.entity.Asset;
import com.ricram.cryptowallet.entity.LatestPrice;
import com.ricram.cryptowallet.entity.Wallet;
import com.ricram.cryptowallet.service.AssetService;
import com.ricram.cryptowallet.service.CoinCapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class AssetServiceImpl implements AssetService {

    private final AssetRepository assetRepository;

    private final WalletRepository walletRepository;

    private final CoinCapService coinCapService;

    private final LatestPriceRepository latestPriceRepository;

    @Override
    @Transactional
    public AssetResponseDto addAsset(Long walletId, AddAssetRequest request) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Wallet not found"));

        AssetInfo info = coinCapService.fetchAssetBySymbol(request.symbol())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Unknown asset symbol: " + request.symbol()
                ));

        LatestPrice latestPrice = latestPriceRepository.findBySlug(info.slug())
                .map(existing -> {
                    existing.setPrice(info.price());
                    existing.setFetchedAt(Instant.now());
                    return existing;
                })
                .orElseGet(() -> LatestPrice.builder()
                        .slug(info.slug())
                        .price(info.price())
                        .fetchedAt(Instant.now())
                        .build()
                );
        latestPriceRepository.save(latestPrice);

        Asset asset = Asset.builder()
                .wallet(wallet)
                .symbol(info.symbol())
                .slug(info.slug())
                .purchasedPrice(request.price())
                .quantity(request.quantity())
                .build();

        Asset savedAsset = assetRepository.save(asset);
        return new AssetResponseDto(savedAsset.getId(), savedAsset.getSymbol(), savedAsset.getPurchasedPrice() /*latestPrice.getPrice()*/, savedAsset.getQuantity());
    }
}
