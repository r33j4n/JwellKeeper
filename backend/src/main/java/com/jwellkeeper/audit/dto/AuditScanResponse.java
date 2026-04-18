package com.jwellkeeper.audit.dto;

public record AuditScanResponse(
        StockAuditResponse audit,
        StockAuditItemResponse item,
        boolean alreadyScanned,
        String message
) {
}
