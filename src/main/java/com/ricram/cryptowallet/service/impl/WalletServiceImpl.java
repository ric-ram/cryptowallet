package com.ricram.cryptowallet.service.impl;

import com.ricram.cryptowallet.dao.WalletRepository;
import com.ricram.cryptowallet.dto.CreateWalletRequest;
import com.ricram.cryptowallet.dto.WalletResponseDto;
import com.ricram.cryptowallet.entity.Wallet;
import com.ricram.cryptowallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;

    @Override
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
}
