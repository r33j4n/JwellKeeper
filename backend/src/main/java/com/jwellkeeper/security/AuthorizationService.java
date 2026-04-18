package com.jwellkeeper.security;

import com.jwellkeeper.common.exception.BadRequestException;
import com.jwellkeeper.common.exception.ForbiddenException;
import com.jwellkeeper.common.exception.ResourceNotFoundException;
import com.jwellkeeper.users.model.UserRole;
import com.jwellkeeper.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthorizationService {

    private static final Set<UserRole> OWNER_MANAGER = EnumSet.of(UserRole.OWNER, UserRole.MANAGER);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public void requireAny(UserRole... roles) {
        UserRole currentRole = TenantContext.requireUser().role();
        if (Arrays.stream(roles).noneMatch(role -> role == currentRole)) {
            throw new ForbiddenException("You do not have permission to perform this action");
        }
    }

    public void requireOwnerOrManager() {
        requireAny(UserRole.OWNER, UserRole.MANAGER);
    }

    public boolean isOwnerOrManager() {
        return OWNER_MANAGER.contains(TenantContext.requireUser().role());
    }

    public void validateCurrentUserPassword(String password, String missingPasswordMessage) {
        var currentUser = TenantContext.requireUser();
        if (password == null || password.isBlank()) {
            throw new BadRequestException(missingPasswordMessage);
        }
        var user = userRepository.findByIdAndTenantId(currentUser.userId(), currentUser.tenantId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BadRequestException("Password is invalid");
        }
    }

    public void validateOwnerOrManagerPassword(String password, String missingPasswordMessage) {
        requireOwnerOrManager();
        validateCurrentUserPassword(password, missingPasswordMessage);
    }

    public void validateOwnerPassword(String password, String missingPasswordMessage) {
        requireAny(UserRole.OWNER);
        validateCurrentUserPassword(password, missingPasswordMessage);
    }
}
