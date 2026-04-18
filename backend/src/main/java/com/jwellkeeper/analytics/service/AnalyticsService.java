package com.jwellkeeper.analytics.service;

import com.jwellkeeper.analytics.dto.AnalyticsRange;
import com.jwellkeeper.analytics.dto.AnalyticsSummaryResponse;
import com.jwellkeeper.analytics.dto.DecisionInsight;
import com.jwellkeeper.analytics.dto.InventoryAgeBucket;
import com.jwellkeeper.analytics.dto.PerformanceRow;
import com.jwellkeeper.analytics.dto.SalesByCurrency;
import com.jwellkeeper.analytics.dto.SalesChartPoint;
import com.jwellkeeper.analytics.dto.SlowMovingItem;
import com.jwellkeeper.billing.model.BillStatus;
import com.jwellkeeper.billing.repository.BillItemRepository;
import com.jwellkeeper.billing.repository.BillRepository;
import com.jwellkeeper.common.exception.BadRequestException;
import com.jwellkeeper.jewellery.model.Jewellery;
import com.jwellkeeper.jewellery.model.JewelleryStatus;
import com.jwellkeeper.jewellery.repository.JewelleryRepository;
import com.jwellkeeper.security.TenantContext;
import com.jwellkeeper.tenant.repository.TenantSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final BillItemRepository billItemRepository;
    private final BillRepository billRepository;
    private final JewelleryRepository jewelleryRepository;
    private final TenantSettingsRepository tenantSettingsRepository;

    private static final Collection<BillStatus> EXCLUDED_BILL_STATUSES = List.of(BillStatus.VOIDED, BillStatus.RETURNED);

    @Transactional(readOnly = true)
    public AnalyticsSummaryResponse summary(AnalyticsRange range, LocalDate from, LocalDate to) {
        UUID tenantId = TenantContext.requireTenantId();
        DateWindow window = resolveWindow(range == null ? AnalyticsRange.THIS_MONTH : range, from, to);
        DateWindow previousWindow = previousWindow(window);

        List<SalesByCurrency> salesByCurrency = salesByCurrency(tenantId, window);
        String currencyCode = salesByCurrency.isEmpty()
                ? tenantSettingsRepository.findById(tenantId).map(s -> s.getDefaultCurrencyCode()).orElse("LKR")
                : salesByCurrency.getFirst().currencyCode();

        List<Object[]> bestTypes = billItemRepository.bestSellingTypes(tenantId, window.from(), window.to());
        String bestSellingType = bestTypes.isEmpty() ? null : (String) bestTypes.getFirst()[0];
        List<Object[]> karats = jewelleryRepository.mostPopularKarat(tenantId, JewelleryStatus.AVAILABLE);
        String mostPopularKarat = karats.isEmpty() ? null : (String) karats.getFirst()[0];
        Double average = billItemRepository.averageSoldWeight(tenantId, window.from(), window.to());
        BigDecimal totalSales = money(billRepository.totalSalesByCurrency(
                tenantId,
                window.from(),
                window.to(),
                currencyCode,
                EXCLUDED_BILL_STATUSES
        ));
        BigDecimal previousSales = money(billRepository.totalSalesByCurrency(
                tenantId,
                previousWindow.from(),
                previousWindow.to(),
                currencyCode,
                EXCLUDED_BILL_STATUSES
        ));
        long billCount = billRepository.billCountByCurrency(
                tenantId,
                window.from(),
                window.to(),
                currencyCode,
                EXCLUDED_BILL_STATUSES
        );
        long totalItemsSold = billItemRepository.totalItemsSold(tenantId, window.from(), window.to());
        BigDecimal totalWeightSold = scaleWeight(billItemRepository.totalWeightSold(tenantId, window.from(), window.to()));
        long availableCount = jewelleryRepository.countByTenantIdAndStatusAndDeletedAtIsNull(tenantId, JewelleryStatus.AVAILABLE);
        BigDecimal availableWeight = scaleWeight(jewelleryRepository.totalWeightByStatus(tenantId, JewelleryStatus.AVAILABLE));
        long missingCount = jewelleryRepository.countByTenantIdAndStatusAndDeletedAtIsNull(tenantId, JewelleryStatus.MISSING);
        BigDecimal missingWeight = scaleWeight(jewelleryRepository.totalWeightByStatus(tenantId, JewelleryStatus.MISSING));
        BigDecimal averageBillValue = billCount == 0 ? BigDecimal.ZERO : totalSales.divide(BigDecimal.valueOf(billCount), 2, RoundingMode.HALF_UP);
        BigDecimal itemsPerBill = billCount == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(totalItemsSold).divide(BigDecimal.valueOf(billCount), 2, RoundingMode.HALF_UP);
        BigDecimal sellThroughRate = percent(BigDecimal.valueOf(totalItemsSold), BigDecimal.valueOf(totalItemsSold + availableCount));
        BigDecimal stockCoverageDays = stockCoverageDays(availableCount, totalItemsSold, window);

        List<SalesChartPoint> chart = billItemRepository.salesByDate(tenantId, window.from(), window.to(), currencyCode)
                .stream()
                .map(row -> new SalesChartPoint(
                        (LocalDate) row[0],
                        (BigDecimal) row[1],
                        ((Number) row[2]).longValue()
                ))
                .toList();
        List<PerformanceRow> typePerformance = performanceRows(
                billItemRepository.typePerformance(tenantId, window.from(), window.to(), currencyCode),
                jewelleryRepository.stockCountByType(tenantId, JewelleryStatus.AVAILABLE)
        );
        List<PerformanceRow> karatPerformance = performanceRows(
                billItemRepository.karatPerformance(tenantId, window.from(), window.to(), currencyCode),
                jewelleryRepository.stockCountByKarat(tenantId, JewelleryStatus.AVAILABLE)
        );
        List<InventoryAgeBucket> ageBuckets = inventoryAgeBuckets(tenantId);
        List<SlowMovingItem> slowMoving = slowMovingItems(tenantId);
        List<DecisionInsight> insights = insights(
                totalSales,
                previousSales,
                missingCount,
                availableCount,
                totalItemsSold,
                stockCoverageDays,
                ageBuckets,
                typePerformance
        );

        return new AnalyticsSummaryResponse(
                window.from(),
                window.to(),
                currencyCode,
                bestSellingType,
                totalSales,
                previousSales,
                salesGrowthPercent(totalSales, previousSales),
                billCount,
                averageBillValue,
                totalWeightSold,
                totalItemsSold,
                availableCount,
                availableWeight,
                missingCount,
                missingWeight,
                mostPopularKarat,
                average == null ? BigDecimal.ZERO : BigDecimal.valueOf(average).setScale(3, RoundingMode.HALF_UP),
                itemsPerBill,
                sellThroughRate,
                stockCoverageDays,
                chart,
                salesByCurrency,
                typePerformance,
                karatPerformance,
                ageBuckets,
                slowMoving,
                insights,
                "Revenue and AOV use the highest-sales currency for this period; sales by currency is split separately to avoid mixing money values."
        );
    }

    private DateWindow resolveWindow(AnalyticsRange range, LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now();
        return switch (range) {
            case THIS_WEEK -> new DateWindow(today.with(DayOfWeek.MONDAY), today);
            case THIS_MONTH -> new DateWindow(today.withDayOfMonth(1), today);
            case LAST_3_MONTHS -> new DateWindow(today.minusMonths(3).plusDays(1), today);
            case ALL_TIME -> new DateWindow(LocalDate.of(1970, 1, 1), today);
            case CUSTOM -> {
                if (from == null || to == null) {
                    throw new BadRequestException("from and to are required for CUSTOM range");
                }
                if (from.isAfter(to)) {
                    throw new BadRequestException("from must be before or equal to to");
                }
                yield new DateWindow(from, to);
            }
        };
    }

    private DateWindow previousWindow(DateWindow window) {
        long days = ChronoUnit.DAYS.between(window.from(), window.to()) + 1;
        LocalDate previousTo = window.from().minusDays(1);
        return new DateWindow(previousTo.minusDays(days - 1), previousTo);
    }

    private List<SalesByCurrency> salesByCurrency(UUID tenantId, DateWindow window) {
        return billRepository.salesByCurrency(tenantId, window.from(), window.to(), EXCLUDED_BILL_STATUSES)
                .stream()
                .map(row -> {
                    String currency = (String) row[0];
                    BigDecimal total = money((BigDecimal) row[1]);
                    long billCount = ((Number) row[2]).longValue();
                    BigDecimal averageBillValue = billCount == 0
                            ? BigDecimal.ZERO
                            : total.divide(BigDecimal.valueOf(billCount), 2, RoundingMode.HALF_UP);
                    return new SalesByCurrency(currency, total, billCount, averageBillValue);
                })
                .toList();
    }

    private List<PerformanceRow> performanceRows(List<Object[]> soldRows, List<Object[]> stockRows) {
        Map<String, Long> availableByName = new LinkedHashMap<>();
        stockRows.forEach(row -> availableByName.put((String) row[0], ((Number) row[1]).longValue()));

        return soldRows.stream()
                .map(row -> {
                    String name = (String) row[0];
                    long itemsSold = ((Number) row[1]).longValue();
                    BigDecimal weightSold = scaleWeight((BigDecimal) row[2]);
                    BigDecimal salesAmount = money((BigDecimal) row[3]);
                    long availableCount = availableByName.getOrDefault(name, 0L);
                    return new PerformanceRow(
                            name,
                            itemsSold,
                            weightSold,
                            salesAmount,
                            availableCount,
                            percent(BigDecimal.valueOf(itemsSold), BigDecimal.valueOf(itemsSold + availableCount))
                    );
                })
                .toList();
    }

    private List<InventoryAgeBucket> inventoryAgeBuckets(UUID tenantId) {
        Instant now = Instant.now();
        Instant day30 = now.minus(30, ChronoUnit.DAYS);
        Instant day60 = now.minus(60, ChronoUnit.DAYS);
        Instant day90 = now.minus(90, ChronoUnit.DAYS);
        Instant start = Instant.EPOCH;
        return List.of(
                ageBucket(tenantId, "0-30 days", day30, now.plusSeconds(1)),
                ageBucket(tenantId, "31-60 days", day60, day30),
                ageBucket(tenantId, "61-90 days", day90, day60),
                ageBucket(tenantId, "90+ days", start, day90)
        );
    }

    private InventoryAgeBucket ageBucket(UUID tenantId, String label, Instant from, Instant to) {
        long count = jewelleryRepository.countByStatusAndCreatedAtRange(
                tenantId,
                JewelleryStatus.AVAILABLE,
                from,
                to
        );
        BigDecimal weight = jewelleryRepository.totalWeightByStatusAndCreatedAtRange(
                tenantId,
                JewelleryStatus.AVAILABLE,
                from,
                to
        );
        return new InventoryAgeBucket(label, count, scaleWeight(weight));
    }

    private List<SlowMovingItem> slowMovingItems(UUID tenantId) {
        Instant threshold = Instant.now().minus(90, ChronoUnit.DAYS);
        return jewelleryRepository.findTop10ByTenantIdAndStatusAndDeletedAtIsNullAndCreatedAtBeforeOrderByCreatedAtAsc(
                        tenantId,
                        JewelleryStatus.AVAILABLE,
                        threshold
                )
                .stream()
                .map(this::slowMovingItem)
                .toList();
    }

    private SlowMovingItem slowMovingItem(Jewellery jewellery) {
        long ageDays = Math.max(0, Duration.between(jewellery.getCreatedAt(), Instant.now()).toDays());
        return new SlowMovingItem(
                jewellery.getId(),
                jewellery.getType() == null ? "Unknown" : jewellery.getType().getName(),
                jewellery.getDesignName(),
                jewellery.getKarat(),
                jewellery.getWeight(),
                jewellery.getCreatedAt(),
                ageDays
        );
    }

    private List<DecisionInsight> insights(
            BigDecimal totalSales,
            BigDecimal previousSales,
            long missingCount,
            long availableCount,
            long totalItemsSold,
            BigDecimal stockCoverageDays,
            List<InventoryAgeBucket> ageBuckets,
            List<PerformanceRow> typePerformance
    ) {
        List<DecisionInsight> base = new java.util.ArrayList<>();
        BigDecimal growth = salesGrowthPercent(totalSales, previousSales);
        if (growth.compareTo(BigDecimal.valueOf(15)) >= 0) {
            base.add(new DecisionInsight(
                    "GOOD",
                    "Sales momentum is improving",
                    "Revenue is up " + growth + "% compared with the previous matching period.",
                    "Review the top type and karat rows before buying more stock; add depth only where stock is also moving."
            ));
        } else if (previousSales.compareTo(BigDecimal.ZERO) > 0 && growth.compareTo(BigDecimal.valueOf(-10)) <= 0) {
            base.add(new DecisionInsight(
                    "WARNING",
                    "Sales are down versus the previous period",
                    "Revenue is " + growth.abs() + "% lower than the previous matching period.",
                    "Check slow-moving designs, review staff conversion notes, and consider targeted promotions before buying similar stock."
            ));
        }
        if (missingCount > 0) {
            base.add(new DecisionInsight(
                    "DANGER",
                    "Missing stock needs owner attention",
                    missingCount + " item(s) are currently marked missing.",
                    "Resolve missing items before trusting stock availability, reorder decisions, or audit accuracy."
            ));
        }
        ageBuckets.stream()
                .filter(bucket -> "90+ days".equals(bucket.label()) && bucket.itemCount() > 0)
                .findFirst()
                .ifPresent(bucket -> base.add(new DecisionInsight(
                        "WARNING",
                        "Old stock is tying up display space",
                        bucket.itemCount() + " available item(s) have been in stock for more than 90 days.",
                        "Review pricing, placement, photography, and staff talking points for these designs."
                )));
        if (totalItemsSold > 0 && stockCoverageDays.compareTo(BigDecimal.valueOf(120)) > 0) {
            base.add(new DecisionInsight(
                    "INFO",
                    "Stock coverage is high",
                    "At the current selling pace, available stock covers about " + stockCoverageDays + " days.",
                    "Avoid broad replenishment; buy only proven fast-moving types or specific customer-requested designs."
            ));
        }
        typePerformance.stream()
                .filter(row -> row.itemsSold() >= 3 && row.availableCount() <= 2)
                .max(Comparator.comparing(PerformanceRow::itemsSold))
                .ifPresent(row -> base.add(new DecisionInsight(
                        "ACTION",
                        "Potential reorder opportunity",
                        row.name() + " sold " + row.itemsSold() + " item(s), with only " + row.availableCount() + " currently available.",
                        "Consider replenishing this type, especially in the best-selling karat and weight range."
                )));
        if (availableCount == 0 && totalItemsSold == 0) {
            base.add(new DecisionInsight(
                    "INFO",
                    "No stock movement yet",
                    "There is not enough sales or stock data in this period to produce strong recommendations.",
                    "Add stock, create bills, and close daily audits to unlock decision insights."
            ));
        }
        return base.stream().limit(6).toList();
    }

    private BigDecimal stockCoverageDays(long availableCount, long totalItemsSold, DateWindow window) {
        long days = Math.max(1, ChronoUnit.DAYS.between(window.from(), window.to()) + 1);
        if (totalItemsSold == 0) {
            return availableCount == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(999);
        }
        BigDecimal averageDailySales = BigDecimal.valueOf(totalItemsSold).divide(BigDecimal.valueOf(days), 6, RoundingMode.HALF_UP);
        return BigDecimal.valueOf(availableCount).divide(averageDailySales, 1, RoundingMode.HALF_UP);
    }

    private BigDecimal salesGrowthPercent(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return current == null || current.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(100);
        }
        return current.subtract(previous).multiply(BigDecimal.valueOf(100)).divide(previous, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal percent(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return numerator.multiply(BigDecimal.valueOf(100)).divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal scaleWeight(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(3, RoundingMode.HALF_UP);
    }

    private record DateWindow(LocalDate from, LocalDate to) {
    }
}
