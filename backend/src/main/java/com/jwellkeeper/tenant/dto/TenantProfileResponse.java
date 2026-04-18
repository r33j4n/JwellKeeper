package com.jwellkeeper.tenant.dto;

import java.util.UUID;

public record TenantProfileResponse(
        UUID id,
        String shopName,
        String shopAddress,
        String shopContactNumber,
        String shopEmail,
        String taxNumber,
        String receiptFooterNote,
        boolean logoAvailable,
        String logoUrl
) {
}
