package com.ricram.cryptowallet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record AddAssetRequest(
        @NotBlank(message = "Symbol must not be empty")
        String symbol,
        @Positive(message = "Price must be positive")
        double price,
        @Positive(message = "Quantity must be positive")
        double quantity
) { }
