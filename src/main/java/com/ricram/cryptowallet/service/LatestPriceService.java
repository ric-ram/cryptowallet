package com.ricram.cryptowallet.service;

import com.ricram.cryptowallet.entity.Asset;

/**
 * Fetches and upserts the latest USD price for every known asset.
 */
public interface LatestPriceService {

    /**
     * Fetches and stores the current price for all assets.
     */
    void fetchAndStoreLatestPrice();
}
