package com.swifteats.analytics.dto;

import java.util.List;
import java.util.Map;

public record WarehouseFailureResponse(
        long warehouseId,
        String warehouseName,
        int month,
        int year,
        int totalFailed,
        Map<String, Long> failureReasonCounts,
        Map<String, Long> warehouseNoteCounts,
        String narrative,
        List<String> recommendations
) {
}
