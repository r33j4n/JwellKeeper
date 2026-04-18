package com.jwellkeeper.billing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WhatsAppSendRequest(
        @NotBlank @Size(max = 40)
        String phoneNumber
) {
}
