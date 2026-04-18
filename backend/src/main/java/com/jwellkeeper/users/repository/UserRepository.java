package com.jwellkeeper.users.repository;

import com.jwellkeeper.users.model.UserAccount;
import com.jwellkeeper.users.model.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserAccount, UUID> {

    Optional<UserAccount> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Optional<UserAccount> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<UserAccount> findByTenantIdAndRole(UUID tenantId, UserRole role, Pageable pageable);

    Page<UserAccount> findByTenantIdAndRoleNot(UUID tenantId, UserRole role, Pageable pageable);
}
