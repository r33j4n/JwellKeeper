package com.jwellkeeper.tenant.repository;

import com.jwellkeeper.tenant.model.TenantSettings;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TenantSettingsRepository extends JpaRepository<TenantSettings, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from TenantSettings s where s.tenantId = :tenantId")
    Optional<TenantSettings> lockByTenantId(@Param("tenantId") UUID tenantId);
}
