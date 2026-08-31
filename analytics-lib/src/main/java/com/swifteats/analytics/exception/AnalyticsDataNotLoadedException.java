package com.swifteats.analytics.exception;

public class AnalyticsDataNotLoadedException extends RuntimeException {

    public AnalyticsDataNotLoadedException() {
        super("Analytics sample data is not loaded. Run POST /api/v1/admin/analytics/import first.");
    }
}
