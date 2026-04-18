package com.jwellkeeper.audit.dto;

import java.math.BigDecimal;

public record AuditReportTypeTally(
        String typeName,
        long todayAddedCount,
        long alreadyAvailableCount,
        long missingCount,
        long currentStockCount,
        long soldTodayCount,
        BigDecimal currentStockWeight
) {
}
