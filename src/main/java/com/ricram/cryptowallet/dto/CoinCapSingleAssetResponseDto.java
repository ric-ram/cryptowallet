package com.ricram.cryptowallet.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CoinCapSingleAssetResponseDto(CoinCapAsset data) {
}
