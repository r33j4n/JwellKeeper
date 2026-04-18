package com.jwellkeeper.billing.dto;

import java.time.Instant;
import java.util.UUID;

public record BillExchangeResponse(
        UUID id,
        UUID billId,
        String billNo,
        String reason,
        UUID createdBy,
        Instant createdAt,
        String message
) {
}
