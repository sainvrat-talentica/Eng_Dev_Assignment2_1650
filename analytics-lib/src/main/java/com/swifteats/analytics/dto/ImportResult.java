package com.swifteats.analytics.dto;

import java.util.Map;

public record ImportResult(
        boolean success,
        String datasetPath,
        Map<String, Long> rowCounts,
        long durationMs,
        String message
) {
    public static ImportResult success(String path, Map<String, Long> counts, long durationMs) {
        return new ImportResult(true, path, counts, durationMs,
                "Sample dataset imported successfully");
    }

    public static ImportResult failure(String path, String message) {
        return new ImportResult(false, path, Map.of(), 0, message);
    }
}
