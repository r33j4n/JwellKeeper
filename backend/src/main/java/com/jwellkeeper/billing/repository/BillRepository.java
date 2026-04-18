package com.jwellkeeper.billing.repository;

import com.jwellkeeper.billing.model.Bill;
import com.jwellkeeper.billing.model.BillStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BillRepository extends JpaRepository<Bill, UUID> {

    @EntityGraph(attributePaths = "items")
    Optional<Bill> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<Bill> findByTenantId(UUID tenantId, Pageable pageable);

    Page<Bill> findByTenantIdAndBillDateBetween(UUID tenantId, LocalDate from, LocalDate to, Pageable pageable);

    @EntityGraph(attributePaths = "items")
    @Query("""
            select distinct b from Bill b
            where b.tenantId = :tenantId
              and (:from is null or b.billDate >= :from)
              and (:to is null or b.billDate <= :to)
              and (
                  :q is null
                  or lower(b.billNo) like :q
                  or lower(b.customerName) like :q
                  or lower(b.customerPhone) like :q
                  or lower(b.notes) like :q
              )
            """)
    Page<Bill> search(
            @Param("tenantId") UUID tenantId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("q") String q,
            Pageable pageable
    );

    @Query("""
            select b.currencyCode, coalesce(sum(b.totalAmount), 0), count(b)
            from Bill b
            where b.tenantId = :tenantId
              and b.billDate between :from and :to
              and b.status not in :excludedStatuses
            group by b.currencyCode
            order by coalesce(sum(b.totalAmount), 0) desc
            """)
    List<Object[]> salesByCurrency(
            @Param("tenantId") UUID tenantId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("excludedStatuses") Collection<BillStatus> excludedStatuses
    );

    @Query("""
            select coalesce(sum(b.totalAmount), 0)
            from Bill b
            where b.tenantId = :tenantId
              and b.billDate between :from and :to
              and b.currencyCode = :currencyCode
              and b.status not in :excludedStatuses
            """)
    BigDecimal totalSalesByCurrency(
            @Param("tenantId") UUID tenantId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("currencyCode") String currencyCode,
            @Param("excludedStatuses") Collection<BillStatus> excludedStatuses
    );

    @Query("""
            select count(b)
            from Bill b
            where b.tenantId = :tenantId
              and b.billDate between :from and :to
              and b.currencyCode = :currencyCode
              and b.status not in :excludedStatuses
            """)
    long billCountByCurrency(
            @Param("tenantId") UUID tenantId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("currencyCode") String currencyCode,
            @Param("excludedStatuses") Collection<BillStatus> excludedStatuses
    );
}
