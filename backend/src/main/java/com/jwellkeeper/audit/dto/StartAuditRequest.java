package com.jwellkeeper.audit.dto;

import java.time.LocalDate;

public record StartAuditRequest(
        LocalDate auditDate,
        String password,
        String repeatReason
) {
}
