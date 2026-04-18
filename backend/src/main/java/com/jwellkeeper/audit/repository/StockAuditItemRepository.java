package com.jwellkeeper.audit.repository;

import com.jwellkeeper.audit.model.AuditItemResolution;
import com.jwellkeeper.audit.model.StockAuditItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockAuditItemRepository extends JpaRepository<StockAuditItem, UUID> {

    Optional<StockAuditItem> findByAuditIdAndTenantIdAndJewelleryId(UUID auditId, UUID tenantId, UUID jewelleryId);

    List<StockAuditItem> findByAuditIdAndTenantId(UUID auditId, UUID tenantId);

    List<StockAuditItem> findByAuditIdAndTenantIdAndScannedFalse(UUID auditId, UUID tenantId);

    List<StockAuditItem> findByAuditIdAndTenantIdAndResolution(UUID auditId, UUID tenantId, AuditItemResolution resolution);

    long countByAuditIdAndTenantIdAndScannedTrue(UUID auditId, UUID tenantId);

    long countByAuditIdAndTenantIdAndScannedFalse(UUID auditId, UUID tenantId);

    long countByAuditIdAndTenantIdAndResolution(UUID auditId, UUID tenantId, AuditItemResolution resolution);
}
