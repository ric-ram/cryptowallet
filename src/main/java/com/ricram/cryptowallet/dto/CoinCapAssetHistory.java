package com.ricram.cryptowallet.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CoinCapAssetHistory(BigDecimal priceUsd, long time, Instant date) {
}
