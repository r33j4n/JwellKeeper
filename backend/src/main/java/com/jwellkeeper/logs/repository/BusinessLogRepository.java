package com.jwellkeeper.logs.repository;

import com.jwellkeeper.logs.model.BusinessLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BusinessLogRepository extends JpaRepository<BusinessLog, UUID> {

    Page<BusinessLog> findByTenantId(UUID tenantId, Pageable pageable);
}
