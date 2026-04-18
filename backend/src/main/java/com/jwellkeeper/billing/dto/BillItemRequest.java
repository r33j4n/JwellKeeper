package com.jwellkeeper.billing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record BillItemRequest(
        @NotNull UUID jewelleryId,

        @NotNull @DecimalMin(value = "0.00")
        BigDecimal finalPrice,

        @DecimalMin(value = "0.00")
        BigDecimal ratePerGram,

        @DecimalMin(value = "0.00")
        BigDecimal makingCharge,

        @DecimalMin(value = "0.00")
        BigDecimal discountAmount,

        @DecimalMin(value = "0.00")
        BigDecimal taxAmount,

        @Size(max = 1000)
        String notes
) {
}
