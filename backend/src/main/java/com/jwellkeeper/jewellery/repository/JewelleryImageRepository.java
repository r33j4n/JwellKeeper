package com.jwellkeeper.jewellery.repository;

import com.jwellkeeper.jewellery.model.JewelleryImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JewelleryImageRepository extends JpaRepository<JewelleryImage, UUID> {

    long countByTenantIdAndJewelleryIdAndDeletedAtIsNull(UUID tenantId, UUID jewelleryId);

    List<JewelleryImage> findByTenantIdAndJewelleryIdAndDeletedAtIsNullOrderBySortOrderAscCreatedAtAsc(UUID tenantId, UUID jewelleryId);

    Optional<JewelleryImage> findByIdAndTenantIdAndJewelleryIdAndDeletedAtIsNull(UUID id, UUID tenantId, UUID jewelleryId);

    Optional<JewelleryImage> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);
}
