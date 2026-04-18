package com.jwellkeeper.audit.dto;

import jakarta.validation.constraints.NotBlank;

public record AuditForceCloseRequest(
        @NotBlank String password,
        @NotBlank String reason
) {
}
