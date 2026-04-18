package com.jwellkeeper.audit.repository;

import com.jwellkeeper.audit.model.StockAudit;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface StockAuditRepository extends JpaRepository<StockAudit, UUID> {

    Page<StockAudit> findByTenantId(UUID tenantId, Pageable pageable);

    Page<StockAudit> findByTenantIdOrderByAuditDateDescRunNumberDesc(UUID tenantId, Pageable pageable);

    @Query("select coalesce(max(a.runNumber), 0) from StockAudit a where a.tenantId = :tenantId and a.auditDate = :auditDate")
    int maxRunNumberForDate(@Param("tenantId") UUID tenantId, @Param("auditDate") LocalDate auditDate);

    boolean existsByTenantIdAndStatus(UUID tenantId, com.jwellkeeper.audit.model.StockAuditStatus status);

    default boolean existsOpenAudit(UUID tenantId) {
        return existsByTenantIdAndStatus(tenantId, com.jwellkeeper.audit.model.StockAuditStatus.OPEN);
    }

    Optional<StockAudit> findTopByTenantIdAndAuditDateOrderByRunNumberDesc(UUID tenantId, LocalDate auditDate);

    @EntityGraph(attributePaths = "items")
    Optional<StockAudit> findTopByTenantIdAndStatusOrderByAuditDateDescRunNumberDesc(UUID tenantId, com.jwellkeeper.audit.model.StockAuditStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "items")
    @Query("select a from StockAudit a where a.id = :id and a.tenantId = :tenantId")
    Optional<StockAudit> lockByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @EntityGraph(attributePaths = "items")
    Optional<StockAudit> findByIdAndTenantId(UUID id, UUID tenantId);
}
