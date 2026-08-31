package com.swifteats.analytics.dto;

import java.util.List;
import java.util.Map;

public record InsightResponse(
        String narrative,
        List<String> recommendations,
        Map<String, Object> evidence
) {
}
