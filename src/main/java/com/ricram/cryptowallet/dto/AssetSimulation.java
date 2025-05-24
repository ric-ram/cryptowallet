package com.ricram.cryptowallet.dto;

import java.math.BigDecimal;

public record AssetSimulation(String symbol, double quantity, BigDecimal value) {
}
