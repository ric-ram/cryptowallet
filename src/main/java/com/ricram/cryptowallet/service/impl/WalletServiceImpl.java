package com.ricram.cryptowallet.service.impl;

import com.ricram.cryptowallet.dao.AssetQuantity;
import com.ricram.cryptowallet.dao.AssetRepository;
import com.ricram.cryptowallet.dao.LatestPriceRepository;
import com.ricram.cryptowallet.dao.WalletRepository;
import com.ricram.cryptowallet.dto.AssetValue;
import com.ricram.cryptowallet.dto.CreateWalletRequest;
import com.ricram.cryptowallet.dto.WalletResponseDto;
import com.ricram.cryptowallet.dto.WalletValuationResponseDto;
import com.ricram.cryptowallet.entity.LatestPrice;
import com.ricram.cryptowallet.entity.Wallet;
import com.ricram.cryptowallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;

    private final AssetRepository assetRepository;

    private final LatestPriceRepository latestPriceRepository;

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

                    BigDecimal price = latestPrice.getPrice();
                    BigDecimal value = price.multiply(BigDecimal.valueOf(quantity));

                    return new AssetValue(symbol, quantity, price, value);
                })
                .toList();

        BigDecimal total = assetValues.stream()
                .map(AssetValue::value)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new WalletValuationResponseDto(walletId, total, assetValues);
    }
}
