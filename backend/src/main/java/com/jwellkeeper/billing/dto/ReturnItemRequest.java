package com.jwellkeeper.billing.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record ReturnItemRequest(
        @NotNull UUID billItemId,
        BigDecimal refundAmount,
        Boolean restock
) {
}
