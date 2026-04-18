package com.jwellkeeper.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AnalyticsSummaryResponse(
        LocalDate from,
        LocalDate to,
        String currencyCode,
        String bestSellingItemType,
        BigDecimal totalSalesAmount,
        BigDecimal previousPeriodSalesAmount,
        BigDecimal salesGrowthPercent,
        long billCount,
        BigDecimal averageBillValue,
        BigDecimal totalWeightSold,
        long totalItemsSold,
        long availableStockCount,
        BigDecimal availableStockWeight,
        long missingStockCount,
        BigDecimal missingStockWeight,
        String mostPopularKarat,
        BigDecimal averageWeightPerSoldItem,
        BigDecimal itemsPerBill,
        BigDecimal sellThroughRate,
        BigDecimal stockCoverageDays,
        List<SalesChartPoint> salesChart,
        List<SalesByCurrency> salesByCurrency,
        List<PerformanceRow> typePerformance,
        List<PerformanceRow> karatPerformance,
        List<InventoryAgeBucket> inventoryAgeBuckets,
        List<SlowMovingItem> slowMovingItems,
        List<DecisionInsight> insights,
        String methodologyNote
) {
}
