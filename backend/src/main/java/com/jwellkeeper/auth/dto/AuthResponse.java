package com.jwellkeeper.auth.dto;

import com.jwellkeeper.users.model.UserRole;

import java.util.UUID;

public record AuthResponse(
        String token,
        UUID userId,
        UUID tenantId,
        UserRole role,
        String email,
        String name,
        String shopName
) {
}
