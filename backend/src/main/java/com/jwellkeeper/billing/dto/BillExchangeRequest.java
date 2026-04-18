package com.jwellkeeper.billing.dto;

import jakarta.validation.constraints.NotBlank;

public record BillExchangeRequest(
        @NotBlank String password,
        @NotBlank String reason
) {
}
