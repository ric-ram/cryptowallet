package com.ricram.cryptowallet.controller;

import com.ricram.cryptowallet.dto.*;
import com.ricram.cryptowallet.service.AssetService;
import com.ricram.cryptowallet.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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

    @Operation(summary = "Create a new wallet",
            description = "Creates a wallet for the given email; emails must be unique",
            responses = {
                @ApiResponse(responseCode = "201", description = "Created",
                    content = @Content(schema = @Schema(implementation = WalletResponseDto.class))),
                @ApiResponse(responseCode = "400", description = "Invalid request payload"),
                @ApiResponse(responseCode = "409", description = "Email already in use")
           })
    @PostMapping
    public ResponseEntity<WalletResponseDto> createWallet(@Valid @RequestBody CreateWalletRequest req) {

        WalletResponseDto dto = walletService.create(req);
        URI location = URI.create("/wallet/" + dto.id());

        return ResponseEntity
                .created(location)
                .body(dto);
    }

    @Operation(summary = "Add an asset to a wallet",
            description = "Validates the symbol against CoinCap, then adds the asset",
            responses = {
                @ApiResponse(responseCode = "201", description = "Asset added",
                    content = @Content(schema = @Schema(implementation = AssetResponseDto.class))),
                @ApiResponse(responseCode = "400", description = "Invalid symbol or request body"),
                @ApiResponse(responseCode = "404", description = "Wallet not found")
            })
    @PostMapping("/{id}/asset")
    public ResponseEntity<AssetResponseDto> addAssetToWallet(@PathVariable("id") Long walletId, @Valid @RequestBody AddAssetRequest req) {

        AssetResponseDto dto = assetService.addAsset(walletId, req);
        URI location = URI.create("/wallet/" + walletId + "/asset/" + dto.id());

        return ResponseEntity
                .created(location)
                .body(dto);
    }

    @Operation(summary = "Get wallet valuation",
            description = "Returns each asset's current price, quantity and total, plus wallet total",
            responses = {
                @ApiResponse(responseCode = "200", description = "Valuation retrieved",
                    content = @Content(schema = @Schema(implementation = WalletValuationResponseDto.class))),
                @ApiResponse(responseCode = "404", description = "Wallet not found"),
                @ApiResponse(responseCode = "503", description = "Price cache unavailable")
            })
    @GetMapping("/{id}")
    public ResponseEntity<WalletValuationResponseDto> getWalletValuation(@PathVariable("id") Long walletId) {
        WalletValuationResponseDto dto = walletService.getValuation(walletId);

        return ResponseEntity.ok(dto);
    }


    @Operation(summary = "Simulate wallet profit/loss",
            description = "Given a date (or today if not provided) and a wallet, computes best/worst performers",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Simulation result",
                            content = @Content(schema = @Schema(implementation = WalletSimulationResponseDto.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid request or missing history data"),
                    @ApiResponse(responseCode = "503", description = "Price service unavailable")
            })
    @PostMapping("/simulate")
    public ResponseEntity<WalletSimulationResponseDto> simulateWallet(@Valid @RequestBody WalletSimulationRequest req) {

        WalletSimulationResponseDto dto = walletService.simulateWallet(req);

        return ResponseEntity.ok(dto);
    }
}
