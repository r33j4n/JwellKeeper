package com.jwellkeeper.audit.model;

public enum AuditItemResolution {
    PENDING,
    FOUND_IN_AUDIT,
    MARKED_MISSING_ON_CLOSE,
    EXCLUDED_BY_RULE
}
