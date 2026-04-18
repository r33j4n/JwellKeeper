package com.jwellkeeper.billing.dto;

import com.jwellkeeper.billing.model.BillStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record BillResponse(
        UUID id,
        String billNo,
        LocalDate billDate,
        BillStatus status,
        String currencyCode,
        BigDecimal totalAmount,
        String customerName,
        String customerPhone,
        String customerAddress,
        String paymentMethod,
        String notes,
        UUID createdBy,
        Instant createdAt,
        long version,
        List<BillItemResponse> items,
        String pdfUrl
) {
}
