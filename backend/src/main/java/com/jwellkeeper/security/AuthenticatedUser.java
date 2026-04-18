package com.jwellkeeper.security;

import com.jwellkeeper.users.model.UserRole;

import java.util.UUID;

public record AuthenticatedUser(UUID userId, UUID tenantId, UserRole role, String email) {
}
