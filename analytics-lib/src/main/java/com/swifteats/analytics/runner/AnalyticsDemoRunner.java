package com.swifteats.analytics.runner;

import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;

import com.swifteats.analytics.dto.InsightQueryRequest;
import com.swifteats.analytics.model.QueryType;
import com.swifteats.analytics.service.AnalyticsQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Runs Assignment 2 sample use cases on startup for local demo / video recording.
 * Enable with {@code swifteats.analytics.demo-on-startup=true}.
 */
@Component
@ConditionalOnProperty(name = "swifteats.analytics.demo-on-startup", havingValue = "true")
@ServiceScope(ServiceName.ANALYTICS)
public class AnalyticsDemoRunner {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsDemoRunner.class);

    private final AnalyticsQueryService queryService;

    public AnalyticsDemoRunner(AnalyticsQueryService queryService) {
        this.queryService = queryService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void runDemoUseCases() {
        log.info("=== SwiftEats Assignment 2 Analytics Demo ===");

        run("UC1 — Delays in Pune yesterday",
                queryService.executeInsightQuery(new InsightQueryRequest(
                        QueryType.DELAY_BY_CITY,
                        Map.of("city", "Pune", "date", "2025-03-17"))));

        run("UC2 — Client 337 failures (past sample week)",
                queryService.executeInsightQuery(new InsightQueryRequest(
                        QueryType.FAILURES_BY_CLIENT,
                        Map.of(
                                "clientId", "337",
                                "from", "2025-04-01",
                                "to", "2025-05-01"))));

        run("UC3 — Warehouse 2 failures in August",
                queryService.executeInsightQuery(new InsightQueryRequest(
                        QueryType.FAILURES_BY_WAREHOUSE,
                        Map.of("warehouseId", "2", "month", "2025-08"))));

        run("UC4 — Pune vs Mumbai failure comparison",
                queryService.executeInsightQuery(new InsightQueryRequest(
                        QueryType.COMPARE_CITIES,
                        Map.of("cityA", "Pune", "cityB", "Mumbai", "month", "2025-08"))));

        run("UC5 — Festival period analysis",
                queryService.executeInsightQuery(new InsightQueryRequest(
                        QueryType.FESTIVAL_ANALYSIS,
                        Map.of("from", "2025-08-01", "to", "2025-09-01"))));

        run("UC6 — Client Y capacity projection (+20k orders)",
                queryService.executeInsightQuery(new InsightQueryRequest(
                        QueryType.CAPACITY_PROJECTION,
                        Map.of("clientId", "337", "additionalMonthlyOrders", "20000"))));

        log.info("=== Analytics demo complete ===");
    }

    private void run(String title, com.swifteats.analytics.dto.InsightResponse response) {
        log.info("");
        log.info("--- {} ---", title);
        log.info("Narrative: {}", response.narrative());
        log.info("Recommendations: {}", response.recommendations());
        log.info("Evidence: {}", response.evidence());
    }
}
