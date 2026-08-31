package com.swifteats.analytics.dto;

import java.util.List;
import java.util.Map;

public record CapacityRiskResponse(
        long clientId,
        String clientName,
        int additionalMonthlyOrders,
        double historicalFailureRate,
        List<WarehouseRisk> warehouseRisks,
        String narrative,
        List<String> recommendations
) {
    public record WarehouseRisk(
            long warehouseId,
            String warehouseName,
            String city,
            int capacity,
            long currentMonthlyOrders,
            long projectedMonthlyOrders,
            double projectedUtilizationPct,
            boolean highRisk
    ) {
    }
}
