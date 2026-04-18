package com.jwellkeeper.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SalesChartPoint(
        LocalDate date,
        BigDecimal totalAmount,
        long itemCount
) {
}
