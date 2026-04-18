package com.jwellkeeper.shop.repository;

import com.jwellkeeper.shop.model.ShopDayStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface ShopDayStatusRepository extends JpaRepository<ShopDayStatus, UUID> {

    Optional<ShopDayStatus> findByTenantIdAndBusinessDate(UUID tenantId, LocalDate businessDate);
}
