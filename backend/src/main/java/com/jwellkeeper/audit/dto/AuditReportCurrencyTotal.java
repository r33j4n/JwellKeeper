package com.jwellkeeper.audit.dto;

import java.math.BigDecimal;

public record AuditReportCurrencyTotal(
        String currencyCode,
        BigDecimal totalAmount,
        long itemCount
) {
}
