package com.jwellkeeper.analytics.dto;

import java.math.BigDecimal;

public record SalesByCurrency(
        String currencyCode,
        BigDecimal totalAmount,
        long billCount,
        BigDecimal averageBillValue
) {
}
