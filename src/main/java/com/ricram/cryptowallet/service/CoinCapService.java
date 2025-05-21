package com.ricram.cryptowallet.service;

import com.ricram.cryptowallet.dto.AssetInfo;

import java.math.BigDecimal;
import java.util.Optional;


/**
 * Defines CoinCap api related business operations
 */
public interface CoinCapService {
    /**
     * Fetches slug, symbol and latest USD price for the given symbol
     *
     * @param symbol case-insensitive asset slug (e.g. "BTC")
     * @return Optional.empty() if not found; otherwise the AssetInfo
     */
    Optional<AssetInfo> fetchAsset(String symbol);

    /**
     * Fetches the lates USD price for the given symbol.
     *
     * @param slug case-insensitive asset slug (e.g. "bitcoin")
     * @return Optional.empty() if CoinCap has no record of the symbol;
     *         otherwise the current prince in USD.
     */
    //Optional<BigDecimal> getLatestPrice(String slug);

}
