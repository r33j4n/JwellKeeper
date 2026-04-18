package com.jwellkeeper.audit.dto;

import java.util.List;

public record AuditAttemptCloseResponse(
        StockAuditResponse audit,
        boolean canCloseCleanly,
        List<StockAuditItemResponse> unresolvedItems
) {
}
