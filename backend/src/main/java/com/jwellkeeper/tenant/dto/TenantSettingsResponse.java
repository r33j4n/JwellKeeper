package com.jwellkeeper.tenant.dto;

public record TenantSettingsResponse(
        String defaultCurrencyCode,
        String billPrefix,
        String billNumberFormat,
        long nextBillSequence,
        String sequenceResetPolicy
) {
}
