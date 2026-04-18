package com.jwellkeeper.audit.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AuditReportSoldRow(
        UUID jewelleryId,
        String billNo,
        LocalDate billDate,
        String typeName,
        String designName,
        String karat,
        BigDecimal weight,
        BigDecimal finalPrice,
        String currencyCode,
        String notes
) {
}
