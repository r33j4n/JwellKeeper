package com.jwellkeeper.audit.dto;

import com.jwellkeeper.audit.model.AuditItemResolution;
import com.jwellkeeper.jewellery.model.JewelleryStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AuditReportStockRow(
        UUID jewelleryId,
        String typeName,
        String designName,
        String karat,
        BigDecimal weight,
        JewelleryStatus status,
        boolean scanned,
        AuditItemResolution resolution,
        Instant createdAt,
        String category
) {
}
