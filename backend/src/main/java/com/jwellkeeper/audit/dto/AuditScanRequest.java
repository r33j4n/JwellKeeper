package com.jwellkeeper.audit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AuditScanRequest(
        @NotNull UUID auditId,
        @NotBlank String token
) {
}
