package com.ricram.cryptowallet.dto;

import java.math.BigDecimal;

public record AssetResponseDto(
        Long id,
        String symbol,
        BigDecimal price,
        double quantity
) { }
