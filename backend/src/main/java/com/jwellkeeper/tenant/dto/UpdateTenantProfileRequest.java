package com.jwellkeeper.tenant.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTenantProfileRequest(
        @NotBlank @Size(max = 160)
        String shopName,

        @Size(max = 500)
        String shopAddress,

        @Size(max = 40)
        String shopContactNumber,

        @Email @Size(max = 180)
        String shopEmail,

        @Size(max = 80)
        String taxNumber,

        @Size(max = 500)
        String receiptFooterNote
) {
}
