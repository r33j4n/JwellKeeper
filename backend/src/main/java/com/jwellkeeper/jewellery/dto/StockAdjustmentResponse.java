package com.jwellkeeper.jewellery.dto;

import com.jwellkeeper.jewellery.model.JewelleryStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record StockAdjustmentResponse(
        UUID id,
        UUID jewelleryId,
        UUID beforeTypeId,
        UUID afterTypeId,
        String beforeKarat,
        String afterKarat,
        BigDecimal beforeWeight,
        BigDecimal afterWeight,
        JewelleryStatus beforeStatus,
        JewelleryStatus afterStatus,
        String reason,
        UUID createdBy,
        Instant createdAt
) {
}
