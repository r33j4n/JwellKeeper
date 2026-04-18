package com.jwellkeeper.audit.dto;

import com.jwellkeeper.audit.model.AuditItemResolution;

import java.time.Instant;
import java.util.UUID;

public record StockAuditItemResponse(
        UUID id,
        UUID jewelleryId,
        boolean scanned,
        Instant scannedAt,
        UUID scannedBy,
        AuditItemResolution resolution,
        Instant resolutionChangedAt
) {
}
