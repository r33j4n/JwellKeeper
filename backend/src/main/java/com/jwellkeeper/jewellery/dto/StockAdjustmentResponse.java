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
        String beforeDesignName,
        String afterDesignName,
        String beforeNotes,
        String afterNotes,
        JewelleryStatus beforeStatus,
        JewelleryStatus afterStatus,
        String reason,
        UUID createdBy,
        Instant createdAt
) {
}
