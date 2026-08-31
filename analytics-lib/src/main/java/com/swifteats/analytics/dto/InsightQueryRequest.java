package com.swifteats.analytics.dto;

import com.swifteats.analytics.model.QueryType;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record InsightQueryRequest(
        @NotNull QueryType queryType,
        Map<String, String> parameters
) {
    public InsightQueryRequest {
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
    }
}
