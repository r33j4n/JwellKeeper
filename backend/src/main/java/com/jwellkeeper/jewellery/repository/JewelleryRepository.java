package com.jwellkeeper.jewellery.repository;

import com.jwellkeeper.jewellery.model.Jewellery;
import com.jwellkeeper.jewellery.model.JewelleryStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JewelleryRepository extends JpaRepository<Jewellery, UUID> {

    @EntityGraph(attributePaths = "type")
    Page<Jewellery> findByTenantIdAndDeletedAtIsNull(UUID tenantId, Pageable pageable);

    @EntityGraph(attributePaths = "type")
    Page<Jewellery> findByTenantIdAndStatusAndDeletedAtIsNull(UUID tenantId, JewelleryStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "type")
    @Query("""
            select j from Jewellery j
            where j.tenantId = :tenantId
              and j.deletedAt is null
              and (:status is null or j.status = :status)
              and (:typeId is null or j.typeId = :typeId)
              and (:karat is null or j.karat = :karat)
              and (:q is null
                  or lower(j.type.name) like :q
                  or lower(j.designName) like :q
                  or lower(j.notes) like :q
                  or lower(j.karat) like :q
                  or exists (
                      select 1 from BillItem bi join bi.bill b
                      where bi.jewelleryId = j.id
                        and bi.tenantId = :tenantId
                        and lower(b.billNo) like :q
                  )
              )
              and (:minWeight is null or j.weight >= :minWeight)
              and (:maxWeight is null or j.weight <= :maxWeight)
            """)
    Page<Jewellery> search(
            @Param("tenantId") UUID tenantId,
            @Param("status") JewelleryStatus status,
            @Param("typeId") UUID typeId,
            @Param("karat") String karat,
            @Param("q") String q,
            @Param("minWeight") java.math.BigDecimal minWeight,
            @Param("maxWeight") java.math.BigDecimal maxWeight,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "type")
    @Query("""
            select j from Jewellery j
            where j.tenantId = :tenantId
              and j.status = com.jwellkeeper.jewellery.model.JewelleryStatus.ARCHIVED
              and j.deletedAt is not null
              and (:typeId is null or j.typeId = :typeId)
              and (:karat is null or j.karat = :karat)
              and (:q is null
                  or lower(j.type.name) like :q
                  or lower(j.designName) like :q
                  or lower(j.notes) like :q
                  or lower(j.karat) like :q
              )
              and (:minWeight is null or j.weight >= :minWeight)
              and (:maxWeight is null or j.weight <= :maxWeight)
            """)
    Page<Jewellery> searchArchived(
            @Param("tenantId") UUID tenantId,
            @Param("typeId") UUID typeId,
            @Param("karat") String karat,
            @Param("q") String q,
            @Param("minWeight") java.math.BigDecimal minWeight,
            @Param("maxWeight") java.math.BigDecimal maxWeight,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "type")
    Optional<Jewellery> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

        @EntityGraph(attributePaths = "type")
        Optional<Jewellery> findByTenantIdAndQrPayloadTokenAndDeletedAtIsNull(UUID tenantId, String qrPayloadToken);

    @EntityGraph(attributePaths = "type")
    List<Jewellery> findByTenantIdAndStatusAndDeletedAtIsNull(UUID tenantId, JewelleryStatus status);

    @EntityGraph(attributePaths = "type")
    List<Jewellery> findByTenantIdAndIdIn(UUID tenantId, List<UUID> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "type")
    @Query("select j from Jewellery j where j.id = :id and j.tenantId = :tenantId and j.deletedAt is null")
    Optional<Jewellery> lockByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    long countByTenantIdAndStatusAndDeletedAtIsNull(UUID tenantId, JewelleryStatus status);

    long countByTenantIdAndStatusAndDeletedAtIsNullAndCreatedAtLessThanEqual(UUID tenantId, JewelleryStatus status, Instant createdAt);

    @Query("""
            select j.karat, count(j)
            from Jewellery j
            where j.tenantId = :tenantId and j.status = :status and j.deletedAt is null
            group by j.karat
            order by count(j) desc
            """)
    List<Object[]> mostPopularKarat(@Param("tenantId") UUID tenantId, @Param("status") JewelleryStatus status);

    @Query("""
            select coalesce(sum(j.weight), 0)
            from Jewellery j
            where j.tenantId = :tenantId and j.status = :status and j.deletedAt is null
            """)
    BigDecimal totalWeightByStatus(@Param("tenantId") UUID tenantId, @Param("status") JewelleryStatus status);

    @Query("""
            select j.type.name, count(j)
            from Jewellery j
            where j.tenantId = :tenantId and j.status = :status and j.deletedAt is null
            group by j.type.name
            """)
    List<Object[]> stockCountByType(@Param("tenantId") UUID tenantId, @Param("status") JewelleryStatus status);

    @Query("""
            select j.karat, count(j)
            from Jewellery j
            where j.tenantId = :tenantId and j.status = :status and j.deletedAt is null
            group by j.karat
            """)
    List<Object[]> stockCountByKarat(@Param("tenantId") UUID tenantId, @Param("status") JewelleryStatus status);

    @Query("""
            select coalesce(sum(j.weight), 0)
            from Jewellery j
            where j.tenantId = :tenantId
              and j.status = :status
              and j.deletedAt is null
              and j.createdAt >= :from
              and j.createdAt < :to
            """)
    BigDecimal totalWeightByStatusAndCreatedAtRange(
            @Param("tenantId") UUID tenantId,
            @Param("status") JewelleryStatus status,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query("""
            select count(j)
            from Jewellery j
            where j.tenantId = :tenantId
              and j.status = :status
              and j.deletedAt is null
              and j.createdAt >= :from
              and j.createdAt < :to
            """)
    long countByStatusAndCreatedAtRange(
            @Param("tenantId") UUID tenantId,
            @Param("status") JewelleryStatus status,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @EntityGraph(attributePaths = "type")
    List<Jewellery> findTop10ByTenantIdAndStatusAndDeletedAtIsNullAndCreatedAtBeforeOrderByCreatedAtAsc(
            UUID tenantId,
            JewelleryStatus status,
            Instant createdAt
    );
}
