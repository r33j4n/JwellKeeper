package com.jwellkeeper.jewellery.dto;

import java.util.UUID;
import java.math.BigDecimal;

public record ArchivedJewelleryRequest(
        String ownerPassword,
        UUID typeId,
        String karat,
        String q,
        BigDecimal minWeight,
        BigDecimal maxWeight
) {
}
