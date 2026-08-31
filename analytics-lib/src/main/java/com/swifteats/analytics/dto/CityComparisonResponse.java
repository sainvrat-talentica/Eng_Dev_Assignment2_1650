package com.swifteats.analytics.dto;

import java.util.List;
import java.util.Map;

public record CityComparisonResponse(
        String cityA,
        String cityB,
        String month,
        Map<String, Long> cityAFailures,
        Map<String, Long> cityBFailures,
        String narrative,
        List<String> recommendations
) {
}
