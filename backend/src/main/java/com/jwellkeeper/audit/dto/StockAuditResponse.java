package com.jwellkeeper.audit.dto;

import com.jwellkeeper.audit.model.StockAuditStatus;
import com.jwellkeeper.audit.model.StockAuditStage;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record StockAuditResponse(
        UUID id,
        LocalDate auditDate,
        int runNumber,
        String auditName,
        StockAuditStatus status,
        StockAuditStage stage,
        boolean manuallyClosed,
        UUID closedBy,
        Instant closedAt,
        boolean forceClosed,
        UUID forceClosedBy,
        Instant forceClosedAt,
        String forceCloseReason,
        String repeatReason,
        UUID repeatOfAuditId,
        long expectedCount,
        BigDecimal expectedTotalWeight,
        long totalItems,
        long scannedItems,
        long missingItems,
        List<StockAuditItemResponse> items,
        String pdfUrl
) {
}
