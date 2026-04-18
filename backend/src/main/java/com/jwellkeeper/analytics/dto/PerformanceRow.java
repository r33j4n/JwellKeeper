package com.jwellkeeper.analytics.dto;

import java.math.BigDecimal;

public record PerformanceRow(
        String name,
        long itemsSold,
        BigDecimal weightSold,
        BigDecimal salesAmount,
        long availableCount,
        BigDecimal sellThroughRate
) {
}
