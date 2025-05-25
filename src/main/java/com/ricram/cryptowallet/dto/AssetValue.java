package com.ricram.cryptowallet.dto;

import java.math.BigDecimal;

public record AssetValue(String symbol, double quantity, BigDecimal price, BigDecimal value) {
}
