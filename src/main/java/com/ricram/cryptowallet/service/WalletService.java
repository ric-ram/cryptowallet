package com.ricram.cryptowallet.service;

import com.ricram.cryptowallet.dto.*;

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

    /**
     * Returns the valuation of a given wallet
     *
     * @param walletId from the wallet to get the valuation from
     * @return the valuation of the wallet with the value of each asset and the total value of the wallet
     */
    WalletValuationResponseDto getValuation(Long walletId);

    /**
     * Returns simulation of a wallet at a given date
     *
     * @param req object with wallet to simulate profit and date to when to calculate it
     * @return the wallet simulation with the total value, worst and best performant asset
     */
    WalletSimulationResponseDto simulate(WalletSimulationRequest req);
}
