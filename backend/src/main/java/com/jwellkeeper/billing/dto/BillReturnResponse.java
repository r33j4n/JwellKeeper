package com.jwellkeeper.billing.dto;

import com.jwellkeeper.billing.model.BillStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BillReturnResponse(
        UUID id,
        UUID billId,
        String billNo,
        BillStatus billStatus,
        BigDecimal refundAmount,
        String reason,
        UUID createdBy,
        Instant createdAt
) {
}
