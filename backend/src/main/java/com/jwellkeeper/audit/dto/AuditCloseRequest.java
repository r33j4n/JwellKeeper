package com.jwellkeeper.audit.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AuditCloseRequest(
        @NotNull UUID auditId,
        String ownerPassword,
        String reason
) {
}
