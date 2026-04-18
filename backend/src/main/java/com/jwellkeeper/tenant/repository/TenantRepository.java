package com.jwellkeeper.tenant.repository;

import com.jwellkeeper.tenant.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {
}
