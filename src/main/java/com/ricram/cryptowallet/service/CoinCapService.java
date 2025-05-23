package com.ricram.cryptowallet.service;

import com.ricram.cryptowallet.dto.AssetInfo;

import java.util.Optional;


/**
 * Defines CoinCap api related business operations
 */
public interface CoinCapService {
    /**
     * Fetches slug, symbol and latest USD price for the given symbol
     *
     * @param symbol case-insensitive asset symbol (e.g. "BTC")
     * @return Optional.empty() if not found; otherwise the AssetInfo
     */
    Optional<AssetInfo> fetchAssetBySymbol(String symbol);


    /**
     * Fetches an asset based on its slug
     *
     * @param slug asset slug (e.g. bitcoin)
     * @return Optional.empty() if not found; otherwise the AssetInfo
     */
    Optional<AssetInfo> fetchAssetBySlug(String slug);
}
