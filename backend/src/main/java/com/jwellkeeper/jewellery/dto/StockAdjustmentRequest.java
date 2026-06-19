package com.jwellkeeper.jewellery.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record StockAdjustmentRequest(
        @NotBlank String password,
        @NotBlank @Size(max = 1000) String reason,
        UUID typeId,
        @Size(max = 16) String karat,
        @Size(max = 160) String designName,
        @Size(max = 1000) String notes,
        @DecimalMin(value = "0.001") BigDecimal weight,
        Boolean archive
) {
}
