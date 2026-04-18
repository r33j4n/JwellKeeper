package com.jwellkeeper.jewellery.repository;

import com.jwellkeeper.jewellery.model.StockAdjustment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StockAdjustmentRepository extends JpaRepository<StockAdjustment, UUID> {

    Page<StockAdjustment> findByTenantIdAndJewelleryIdOrderByCreatedAtDesc(UUID tenantId, UUID jewelleryId, Pageable pageable);
}
