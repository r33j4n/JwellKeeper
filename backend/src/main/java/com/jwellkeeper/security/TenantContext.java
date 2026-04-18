package com.jwellkeeper.security;

import com.jwellkeeper.common.exception.UnauthorizedException;

import java.util.Optional;
import java.util.UUID;

public final class TenantContext {

    private static final ThreadLocal<AuthenticatedUser> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(AuthenticatedUser user) {
        CURRENT.set(user);
    }

    public static Optional<AuthenticatedUser> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static AuthenticatedUser requireUser() {
        return current().orElseThrow(() -> new UnauthorizedException("Authenticated user is required"));
    }

    public static UUID requireTenantId() {
        return requireUser().tenantId();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
