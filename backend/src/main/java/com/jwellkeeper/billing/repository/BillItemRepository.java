package com.jwellkeeper.billing.repository;

import com.jwellkeeper.billing.model.BillItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface BillItemRepository extends JpaRepository<BillItem, UUID> {

    @Query("""
            select coalesce(sum(i.weight), 0)
            from BillItem i join i.bill b
            where i.tenantId = :tenantId and b.billDate between :from and :to
              and b.status <> com.jwellkeeper.billing.model.BillStatus.VOIDED
              and b.status <> com.jwellkeeper.billing.model.BillStatus.RETURNED
            """)
    BigDecimal totalWeightSold(@Param("tenantId") UUID tenantId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            select count(i)
            from BillItem i join i.bill b
            where i.tenantId = :tenantId and b.billDate between :from and :to
              and b.status <> com.jwellkeeper.billing.model.BillStatus.VOIDED
              and b.status <> com.jwellkeeper.billing.model.BillStatus.RETURNED
            """)
    long totalItemsSold(@Param("tenantId") UUID tenantId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            select coalesce(sum(i.finalPrice), 0)
            from BillItem i join i.bill b
            where i.tenantId = :tenantId and b.billDate between :from and :to
              and b.status <> com.jwellkeeper.billing.model.BillStatus.VOIDED
              and b.status <> com.jwellkeeper.billing.model.BillStatus.RETURNED
            """)
    BigDecimal totalSales(@Param("tenantId") UUID tenantId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            select coalesce(avg(i.weight), 0)
            from BillItem i join i.bill b
            where i.tenantId = :tenantId and b.billDate between :from and :to
              and b.status <> com.jwellkeeper.billing.model.BillStatus.VOIDED
              and b.status <> com.jwellkeeper.billing.model.BillStatus.RETURNED
            """)
    Double averageSoldWeight(@Param("tenantId") UUID tenantId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            select i.typeNameSnapshot, count(i)
            from BillItem i join i.bill b
            where i.tenantId = :tenantId and b.billDate between :from and :to
              and b.status <> com.jwellkeeper.billing.model.BillStatus.VOIDED
              and b.status <> com.jwellkeeper.billing.model.BillStatus.RETURNED
            group by i.typeNameSnapshot
            order by count(i) desc
            """)
    List<Object[]> bestSellingTypes(@Param("tenantId") UUID tenantId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            select b.billDate, coalesce(sum(i.finalPrice), 0), count(i)
            from BillItem i join i.bill b
            where i.tenantId = :tenantId and b.billDate between :from and :to
              and b.currencyCode = :currencyCode
              and b.status <> com.jwellkeeper.billing.model.BillStatus.VOIDED
              and b.status <> com.jwellkeeper.billing.model.BillStatus.RETURNED
            group by b.billDate
            order by b.billDate asc
            """)
    List<Object[]> salesByDate(
            @Param("tenantId") UUID tenantId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("currencyCode") String currencyCode
    );

    @Query("""
            select i.typeNameSnapshot, count(i), coalesce(sum(i.weight), 0), coalesce(sum(i.finalPrice), 0)
            from BillItem i join i.bill b
            where i.tenantId = :tenantId
              and b.billDate between :from and :to
              and b.currencyCode = :currencyCode
              and b.status <> com.jwellkeeper.billing.model.BillStatus.VOIDED
              and b.status <> com.jwellkeeper.billing.model.BillStatus.RETURNED
            group by i.typeNameSnapshot
            order by count(i) desc, coalesce(sum(i.finalPrice), 0) desc
            """)
    List<Object[]> typePerformance(
            @Param("tenantId") UUID tenantId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("currencyCode") String currencyCode
    );

    @Query("""
            select i.karatSnapshot, count(i), coalesce(sum(i.weight), 0), coalesce(sum(i.finalPrice), 0)
            from BillItem i join i.bill b
            where i.tenantId = :tenantId
              and b.billDate between :from and :to
              and b.currencyCode = :currencyCode
              and b.status <> com.jwellkeeper.billing.model.BillStatus.VOIDED
              and b.status <> com.jwellkeeper.billing.model.BillStatus.RETURNED
            group by i.karatSnapshot
            order by count(i) desc, coalesce(sum(i.finalPrice), 0) desc
            """)
    List<Object[]> karatPerformance(
            @Param("tenantId") UUID tenantId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("currencyCode") String currencyCode
    );

    @Query("""
            select i.jewelleryId, b.id, b.billNo
            from BillItem i join i.bill b
            where i.tenantId = :tenantId and i.jewelleryId in :jewelleryIds
            """)
    List<Object[]> billNumbersByJewelleryIds(@Param("tenantId") UUID tenantId, @Param("jewelleryIds") Collection<UUID> jewelleryIds);

    @Query("""
            select i
            from BillItem i join fetch i.bill b
            where i.tenantId = :tenantId
              and b.billDate = :billDate
              and b.status <> com.jwellkeeper.billing.model.BillStatus.VOIDED
              and b.status <> com.jwellkeeper.billing.model.BillStatus.RETURNED
            order by b.billNo asc, i.typeNameSnapshot asc
            """)
    List<BillItem> soldItemsForDate(@Param("tenantId") UUID tenantId, @Param("billDate") LocalDate billDate);
}
