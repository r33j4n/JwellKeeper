package com.jwellkeeper.logs.dto;

import java.time.Instant;
import java.util.UUID;

public record BusinessLogResponse(
        UUID id,
        UUID actorUserId,
        String action,
        String entityType,
        UUID entityId,
        String result,
        String message,
        String metadata,
        String ipAddress,
        String userAgent,
        Instant createdAt
) {
}
