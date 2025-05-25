package com.ricram.cryptowallet.dto;

import java.math.BigDecimal;

public record AssetProfitSimulation(
        String symbol,
        double quantity,
        BigDecimal value,
        BigDecimal profitAmount,
        BigDecimal profitPercent)
{ }
