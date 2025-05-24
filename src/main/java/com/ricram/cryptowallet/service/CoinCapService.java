package com.ricram.cryptowallet.service;

import com.ricram.cryptowallet.dto.AssetInfo;
import com.ricram.cryptowallet.dto.CoinCapAssetHistory;
import com.ricram.cryptowallet.dto.CoinCapHistoryResponseDto;

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

    /**
     * Fetches an asset history between 2 dates based on its slug.
     * The interval of results is by default 1 day.
     *
     * @param slug asset slug (e.g. bitcoin)
     * @param startMillis start date in UNIX time in milliseconds
     * @param endMillis end date in UNIX time in milliseconds
     * @return Optional.empty() if no history found;
     *         Otherwise a list with the price history.
     */
    Optional<CoinCapHistoryResponseDto> fetchAssetHistoryBySlug(String slug, long startMillis, long endMillis);
}
