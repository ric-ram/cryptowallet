package com.ricram.cryptowallet.dto;

import java.math.BigDecimal;

public record WalletSimulationResponseDto(
        BigDecimal total,
        String bestAsset,
        BigDecimal bestPerformance,
        String worstAsset,
        BigDecimal worstPerformance)
{ }
