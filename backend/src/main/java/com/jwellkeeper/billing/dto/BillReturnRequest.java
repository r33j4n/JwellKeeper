package com.jwellkeeper.billing.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record BillReturnRequest(
        @NotBlank String password,
        @NotBlank String reason,
        @NotEmpty List<@Valid ReturnItemRequest> items
) {
}
