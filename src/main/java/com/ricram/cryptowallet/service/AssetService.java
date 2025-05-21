package com.ricram.cryptowallet.service;

import com.ricram.cryptowallet.dto.AddAssetRequest;
import com.ricram.cryptowallet.dto.AssetResponseDto;

/**
 * Defines asset-related business operations
 */
public interface AssetService {
    /**
     * Add an asset to an existing wallet
     *
     * @param walletId the id for the wallet to add the asset to
     * @param request payload containing the asset information
     * @return the added asset to the desired wallet
     */
    AssetResponseDto addAsset(Long walletId, AddAssetRequest request);
}
