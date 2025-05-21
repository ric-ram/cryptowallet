package com.ricram.cryptowallet.dto;

public record AssetResponseDto(
        Long id,
        String symbol,
        double price,
        double quantity
) { }
