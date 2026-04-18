package com.jwellkeeper.shop.dto;

import com.jwellkeeper.shop.model.ShopStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ShopStateResponse(
        UUID id,
        LocalDate businessDate,
        ShopStatus status,
        UUID openedBy,
        Instant openedAt,
        UUID closedBy,
        Instant closedAt,
        long version
) {
}
