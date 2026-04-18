package com.jwellkeeper.jewellery.repository;

import com.jwellkeeper.jewellery.model.JewelleryType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JewelleryTypeRepository extends JpaRepository<JewelleryType, UUID> {

    boolean existsByTenantIdAndNameIgnoreCase(UUID tenantId, String name);

    Optional<JewelleryType> findByIdAndTenantId(UUID id, UUID tenantId);

    List<JewelleryType> findByTenantIdOrderByNameAsc(UUID tenantId);
}
