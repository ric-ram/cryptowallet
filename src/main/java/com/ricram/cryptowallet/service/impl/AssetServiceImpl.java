package com.ricram.cryptowallet.service.impl;

import com.ricram.cryptowallet.dao.AssetRepository;
import com.ricram.cryptowallet.dao.WalletRepository;
import com.ricram.cryptowallet.dto.AddAssetRequest;
import com.ricram.cryptowallet.dto.AssetResponseDto;
import com.ricram.cryptowallet.entity.Asset;
import com.ricram.cryptowallet.entity.Wallet;
import com.ricram.cryptowallet.service.AssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class AssetServiceImpl implements AssetService {

    private final AssetRepository assetRepository;
    private final WalletRepository walletRepository;

    @Override
    @Transactional
    public AssetResponseDto addAsset(Long walletId, AddAssetRequest request) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Wallet not found"));

        // TODO: validate symbol and its price via CoinCapService

        Asset asset = Asset.builder()
                .wallet(wallet)
                .symbol(request.symbol().toUpperCase())
                .price(request.price())
                .quantity(request.quantity())
                .build();

        Asset savedAsset = assetRepository.save(asset);
        return new AssetResponseDto(asset.getId(), asset.getSymbol(), asset.getPrice(), asset.getQuantity());
    }
}
