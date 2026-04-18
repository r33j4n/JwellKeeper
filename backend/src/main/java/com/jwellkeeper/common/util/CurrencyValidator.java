package com.jwellkeeper.common.util;

import com.jwellkeeper.common.exception.BadRequestException;

import java.util.Currency;

public final class CurrencyValidator {

    private CurrencyValidator() {
    }

    public static String requireIsoCurrency(String currencyCode) {
        if (currencyCode == null || currencyCode.isBlank()) {
            throw new BadRequestException("currencyCode is required");
        }
        String normalized = currencyCode.trim().toUpperCase();
        try {
            Currency.getInstance(normalized);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("currencyCode must be a valid ISO-4217 code");
        }
        return normalized;
    }
}
