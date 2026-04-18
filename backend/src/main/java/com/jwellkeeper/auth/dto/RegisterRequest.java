package com.jwellkeeper.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 160)
        String shopName,

        @NotBlank @Size(max = 140)
        String ownerName,

        @NotBlank @Email @Size(max = 180)
        String email,

        @NotBlank @Size(min = 8, max = 100)
        String password,

        @NotBlank @Size(min = 1, max = 20)
        String billPrefix,

        @NotBlank @Pattern(regexp = "^[A-Za-z]{3}$", message = "defaultCurrencyCode must be an ISO-4217 code")
        String defaultCurrencyCode
) {
}
