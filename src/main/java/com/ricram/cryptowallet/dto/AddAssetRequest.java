package com.ricram.cryptowallet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record AddAssetRequest(
        @NotBlank(message = "Symbol must not be empty")
        String symbol,
        @Positive(message = "Price must be positive")
        BigDecimal price,
        @Positive(message = "Quantity must be positive")
        double quantity
) { }
