package com.jwellkeeper.billing.repository;

import com.jwellkeeper.billing.model.BillVoid;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BillVoidRepository extends JpaRepository<BillVoid, UUID> {

    boolean existsByTenantIdAndBillId(UUID tenantId, UUID billId);
}
