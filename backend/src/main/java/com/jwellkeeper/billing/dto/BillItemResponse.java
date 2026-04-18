package com.jwellkeeper.billing.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record BillItemResponse(
        UUID id,
        UUID jewelleryId,
        String typeNameSnapshot,
        String designNameSnapshot,
        String karatSnapshot,
        BigDecimal weight,
        BigDecimal finalPrice,
        BigDecimal ratePerGram,
        BigDecimal makingCharge,
        BigDecimal discountAmount,
        BigDecimal taxAmount,
        String notes
) {
}
