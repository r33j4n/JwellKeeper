package com.jwellkeeper.jewellery.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateJewelleryRequest(
        @NotNull UUID typeId,

        @NotBlank @Size(max = 16)
        String karat,

        @Size(max = 160)
        String designName,

        @NotNull @DecimalMin(value = "0.001")
        BigDecimal weight,

        @Size(max = 1000)
        String notes
) {
}
