package com.ricram.cryptowallet.dto;

import java.math.BigDecimal;

public record AssetInfo(String slug, String symbol, BigDecimal price) {
}
