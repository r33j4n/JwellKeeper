package com.jwellkeeper.analytics.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SlowMovingItem(
        UUID jewelleryId,
        String typeName,
        String designName,
        String karat,
        BigDecimal weight,
        Instant createdAt,
        long ageDays
) {
}
