package com.jwellkeeper.tenant.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateTenantSettingsRequest(
        @Pattern(regexp = "^[A-Z]{3}$", message = "currencyCode must be an ISO-4217 code")
        String defaultCurrencyCode,

        @Size(min = 1, max = 20)
        String billPrefix,

        @Size(min = 1, max = 80)
        String billNumberFormat,

        Long nextBillSequence
) {
}
