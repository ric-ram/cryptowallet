package com.ricram.cryptowallet.service;

import com.ricram.cryptowallet.dto.CreateWalletRequest;
import com.ricram.cryptowallet.dto.WalletResponseDto;

/**
 * Defines wallet-related business operations
 */
public interface WalletService {

    /**
     * Create a new wallet for the given email
     *
     * @param request with the email in the payload
     * @return the created wallet, with its generated ID
     */
    WalletResponseDto create(CreateWalletRequest request);
}
