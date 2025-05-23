package com.ricram.cryptowallet.controller;

import com.ricram.cryptowallet.dto.*;
import com.ricram.cryptowallet.service.AssetService;
import com.ricram.cryptowallet.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/wallet")
@RequiredArgsConstructor
@Validated
public class WalletController {

    private final WalletService walletService;
    private final AssetService assetService;

    @PostMapping
    public ResponseEntity<WalletResponseDto> createWallet(@Valid @RequestBody CreateWalletRequest req) {

        WalletResponseDto dto = walletService.create(req);
        URI location = URI.create("/wallet/" + dto.id());

        return ResponseEntity
                .created(location)
                .body(dto);
    }

    @PostMapping("/{id}/asset")
    public ResponseEntity<AssetResponseDto> addAssetToWallet(@PathVariable("id") Long walletId, @Valid @RequestBody AddAssetRequest req) {

        AssetResponseDto dto = assetService.addAsset(walletId, req);
        URI location = URI.create("/wallet/" + walletId + "/asset/" + dto.id());

        return ResponseEntity
                .created(location)
                .body(dto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WalletValuationResponseDto> getWalletValuation(@PathVariable("id") Long walletId) {
        WalletValuationResponseDto dto = walletService.getValuation(walletId);

        return ResponseEntity.ok(dto);
    }
}
