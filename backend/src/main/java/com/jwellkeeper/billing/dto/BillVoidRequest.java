package com.jwellkeeper.billing.dto;

import jakarta.validation.constraints.NotBlank;

public record BillVoidRequest(
        @NotBlank String password,
        @NotBlank String reason
) {
}
