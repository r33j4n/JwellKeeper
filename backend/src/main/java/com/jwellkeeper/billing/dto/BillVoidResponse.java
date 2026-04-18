package com.jwellkeeper.billing.dto;

import com.jwellkeeper.billing.model.BillStatus;

import java.time.Instant;
import java.util.UUID;

public record BillVoidResponse(
        UUID id,
        UUID billId,
        String billNo,
        BillStatus billStatus,
        String reason,
        UUID createdBy,
        Instant createdAt
) {
}
