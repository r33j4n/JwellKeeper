package com.jwellkeeper.jewellery.dto;

import com.jwellkeeper.jewellery.model.JewelleryStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record JewelleryResponse(
        UUID id,
        UUID typeId,
        String typeName,
        String karat,
        String designName,
        BigDecimal weight,
        JewelleryStatus status,
        String notes,
        UUID billId,
        String billNo,
        Instant createdAt,
        Instant soldAt,
        Instant deletedAt,
        long version
) {
}
