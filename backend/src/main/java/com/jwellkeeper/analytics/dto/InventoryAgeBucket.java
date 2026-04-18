package com.jwellkeeper.analytics.dto;

import java.math.BigDecimal;

public record InventoryAgeBucket(
        String label,
        long itemCount,
        BigDecimal totalWeight
) {
}
