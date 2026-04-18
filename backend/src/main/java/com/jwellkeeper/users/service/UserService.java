package com.jwellkeeper.users.service;

import com.jwellkeeper.common.exception.ConflictException;
import com.jwellkeeper.common.exception.ResourceNotFoundException;
import com.jwellkeeper.common.pagination.PageResponse;
import com.jwellkeeper.logs.service.BusinessLogService;
import com.jwellkeeper.security.TenantContext;
import com.jwellkeeper.users.dto.CreateStaffRequest;
import com.jwellkeeper.users.dto.UpdateStaffRequest;
import com.jwellkeeper.users.dto.UserResponse;
import com.jwellkeeper.users.model.UserAccount;
import com.jwellkeeper.users.model.UserRole;
import com.jwellkeeper.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final BusinessLogService logService;

    @Transactional
    public UserResponse createStaff(CreateStaffRequest request) {
        if (repository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("Email is already registered");
        }
        UserAccount staff = new UserAccount();
        staff.setTenantId(TenantContext.requireTenantId());
        staff.setName(request.name().trim());
        staff.setEmail(request.email().trim().toLowerCase());
        staff.setPasswordHash(passwordEncoder.encode(request.password()));
        staff.setRole(normalizeStaffRole(request.role()));
        staff.setActive(true);
        repository.save(staff);
        logService.log("STAFF_CREATED", "User", staff.getId(), "SUCCESS", "Staff user created", Map.of("email", staff.getEmail()));
        return toResponse(staff);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> listStaff(Pageable pageable) {
        return PageResponse.from(repository.findByTenantIdAndRoleNot(TenantContext.requireTenantId(), UserRole.OWNER, pageable).map(this::toResponse));
    }

    @Transactional
    public UserResponse updateStaff(UUID id, UpdateStaffRequest request) {
        UserAccount staff = repository.findByIdAndTenantId(id, TenantContext.requireTenantId())
                .filter(user -> user.getRole() != UserRole.OWNER)
                .orElseThrow(() -> new ResourceNotFoundException("Staff user not found"));

        if (request.email() != null && !request.email().equalsIgnoreCase(staff.getEmail())) {
            if (repository.existsByEmailIgnoreCase(request.email())) {
                throw new ConflictException("Email is already registered");
            }
            staff.setEmail(request.email().trim().toLowerCase());
        }
        if (request.name() != null && !request.name().isBlank()) {
            staff.setName(request.name().trim());
        }
        if (request.password() != null && !request.password().isBlank()) {
            staff.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        if (request.active() != null) {
            staff.setActive(request.active());
        }
        if (request.role() != null) {
            staff.setRole(normalizeStaffRole(request.role()));
        }
        logService.log("STAFF_UPDATED", "User", staff.getId(), "SUCCESS", "Staff user updated", Map.of("email", staff.getEmail()));
        return toResponse(staff);
    }

    @Transactional
    public void deactivateStaff(UUID id) {
        UserAccount staff = repository.findByIdAndTenantId(id, TenantContext.requireTenantId())
                .filter(user -> user.getRole() != UserRole.OWNER)
                .orElseThrow(() -> new ResourceNotFoundException("Staff user not found"));
        staff.setActive(false);
        logService.log("STAFF_DEACTIVATED", "User", staff.getId(), "SUCCESS", "Staff user deactivated", Map.of("email", staff.getEmail()));
    }

    private UserResponse toResponse(UserAccount user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.isActive(), user.getCreatedAt());
    }

    private UserRole normalizeStaffRole(UserRole role) {
        UserRole normalized = role == null ? UserRole.STAFF : role;
        if (normalized == UserRole.OWNER) {
            throw new ConflictException("Owner role cannot be assigned through staff management");
        }
        return normalized;
    }
}
