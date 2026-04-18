package com.jwellkeeper.billing.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record CreateBillRequest(
        LocalDate billDate,

        @Pattern(regexp = "^[A-Za-z]{3}$", message = "currencyCode must be an ISO-4217 code")
        String currencyCode,

        @Size(max = 160)
        String customerName,

        @Size(max = 40)
        String customerPhone,

        @Size(max = 500)
        String customerAddress,

        @Size(max = 80)
        String paymentMethod,

        @Size(max = 1000)
        String notes,

        @NotEmpty
        List<@Valid BillItemRequest> items
) {
}
