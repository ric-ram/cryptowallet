package com.ricram.cryptowallet.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CoinCapHistoryResponseDto(List<CoinCapAssetHistory> data) {
}
