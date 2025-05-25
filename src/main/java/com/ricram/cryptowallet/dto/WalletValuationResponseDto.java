package com.ricram.cryptowallet.dto;

import java.math.BigDecimal;
import java.util.List;

public record WalletValuationResponseDto(Long id, BigDecimal total, List<AssetValue> assets) {
}
