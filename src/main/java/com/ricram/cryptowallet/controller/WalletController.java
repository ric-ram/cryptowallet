package com.ricram.cryptowallet.controller;

import com.ricram.cryptowallet.dto.CreateWalletRequest;
import com.ricram.cryptowallet.dto.WalletResponseDto;
import com.ricram.cryptowallet.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/wallet")
@RequiredArgsConstructor
@Validated
public class WalletController {

    private final WalletService walletService;

    @PostMapping
    public ResponseEntity<WalletResponseDto> createWallet(@Valid @RequestBody CreateWalletRequest req) {

        WalletResponseDto dto = walletService.create(req);
        URI location = URI.create("/wallet/" + dto.id());

        return ResponseEntity
                .created(location)
                .body(dto);
    }
}
