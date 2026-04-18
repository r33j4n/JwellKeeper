package com.jwellkeeper.audit.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AuditReportResponse(
        UUID auditId,
        LocalDate auditDate,
        int runNumber,
        String auditName,
        long beforeSalesStock,
        long afterSalesExpectedStock,
        long scannedItems,
        long missingItems,
        long itemsSoldToday,
        BigDecimal salesTotalToday,
        List<AuditReportStockRow> currentStockItems,
        List<AuditReportSoldRow> todaySoldItems,
        List<AuditReportTypeTally> typeTallies,
        List<AuditReportCurrencyTotal> salesTotalsByCurrency,
        String pdfUrl
) {
}
