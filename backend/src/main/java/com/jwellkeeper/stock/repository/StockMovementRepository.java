package com.jwellkeeper.stock.repository;

import com.jwellkeeper.stock.model.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {

    Page<StockMovement> findByTenantIdAndJewelleryIdOrderByCreatedAtDesc(UUID tenantId, UUID jewelleryId, Pageable pageable);
}
