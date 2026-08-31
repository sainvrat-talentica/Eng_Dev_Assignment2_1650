package com.swifteats.analytics.dto;

import java.util.Map;

public record DelayAnalysisResponse(
        String city,
        String date,
        int totalAffected,
        Map<String, Long> failureReasonCounts,
        long heavyTrafficCount,
        long slowPackingCount,
        long negativeFeedbackCount,
        String narrative,
        java.util.List<String> recommendations
) {
}
