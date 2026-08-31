package com.swifteats.analytics.dto;

import java.util.List;
import java.util.Map;

public record FailureAnalysisResponse(
        long clientId,
        String from,
        String to,
        int totalFailed,
        List<FailureReasonBreakdown> breakdown,
        String narrative,
        List<String> recommendations
) {
    public record FailureReasonBreakdown(
            String failureReason,
            long count,
            String fleetIssues,
            String warehouseIssues
    ) {
    }
}
