package com.swifteats.analytics.engine;

import com.swifteats.analytics.dto.CapacityRiskResponse;
import com.swifteats.analytics.dto.CityComparisonResponse;
import com.swifteats.analytics.dto.DelayAnalysisResponse;
import com.swifteats.analytics.dto.FailureAnalysisResponse;
import com.swifteats.analytics.dto.WarehouseFailureResponse;
import com.swifteats.analytics.model.CorrelationRuleMatch;
import com.swifteats.analytics.repository.AnalyticsOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InsightGeneratorTest {

    private InsightGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new InsightGenerator();
    }

    @Test
    void delayNarrative_includesCityDateAndMetrics() {
        Map<String, Long> reasons = new LinkedHashMap<>();
        reasons.put("Traffic congestion", 6L);
        reasons.put("Stockout", 4L);

        DelayAnalysisResponse result = new DelayAnalysisResponse(
                "Pune",
                "2025-08-15",
                10,
                reasons,
                3L,
                2L,
                1L,
                null,
                List.of());

        String narrative = generator.delayNarrative(result);

        assertThat(narrative)
                .contains("Pune")
                .contains("2025-08-15")
                .contains("10 deliveries were delayed or failed")
                .contains("Traffic congestion")
                .contains("60%")
                .contains("3 orders correlated with heavy traffic")
                .contains("2 with slow warehouse packing")
                .contains("1 customers left negative feedback");
    }

    @Test
    void clientFailureNarrative_whenNoFailures() {
        FailureAnalysisResponse result = new FailureAnalysisResponse(
                42L, "2025-01-01", "2025-01-31", 0, List.of(), null, List.of());

        String narrative = generator.clientFailureNarrative(result, "Acme Foods");

        assertThat(narrative)
                .isEqualTo("Client Acme Foods (id 42) had no failed orders between 2025-01-01 and 2025-01-31.");
    }

    @Test
    void clientFailureNarrative_whenFailuresPresent() {
        FailureAnalysisResponse.FailureReasonBreakdown breakdown = new FailureAnalysisResponse.FailureReasonBreakdown(
                "Stockout", 5L, "Heavy congestion", null);
        FailureAnalysisResponse result = new FailureAnalysisResponse(
                42L, "2025-01-01", "2025-01-31", 5, List.of(breakdown), null, List.of());

        String narrative = generator.clientFailureNarrative(result, "Acme Foods");

        assertThat(narrative)
                .contains("Client Acme Foods (id 42) recorded 5 failed orders")
                .contains("Primary failure reason: Stockout (5 orders)")
                .contains("Fleet issues noted: Heavy congestion")
                .contains("Warehouse issues: —");
    }

    @Test
    void warehouseFailureNarrative_summarizesFailuresAndNotes() {
        Map<String, Long> reasons = new LinkedHashMap<>();
        reasons.put("Warehouse delay", 8L);
        Map<String, Long> notes = new LinkedHashMap<>();
        notes.put("Packing backlog", 3L);
        notes.put("Staff shortage", 2L);

        WarehouseFailureResponse result = new WarehouseFailureResponse(
                7L, "WH-Pune-1", 8, 2025, 8, reasons, notes, null, List.of());

        String narrative = generator.warehouseFailureNarrative(result);

        assertThat(narrative)
                .contains("Warehouse WH-Pune-1 (id 7) had 8 failed orders in 2025-08")
                .contains("Leading failure reason: Warehouse delay")
                .contains("Packing backlog (3)")
                .contains("Staff shortage (2)");
    }

    @Test
    void cityComparisonNarrative_comparesBothCities() {
        Map<String, Long> cityA = new LinkedHashMap<>();
        cityA.put("Incorrect address", 4L);
        Map<String, Long> cityB = new LinkedHashMap<>();
        cityB.put("Warehouse delay", 6L);

        CityComparisonResponse result = new CityComparisonResponse(
                "Pune", "Mumbai", "2025-07", cityA, cityB, null, List.of());

        String narrative = generator.cityComparisonNarrative(result);

        assertThat(narrative)
                .contains("During 2025-07")
                .contains("Pune had 4 failed deliveries (top cause: Incorrect address)")
                .contains("Mumbai had 6 (top cause: Warehouse delay)");
    }

    @Test
    void festivalNarrative_whenEmpty() {
        String narrative = generator.festivalNarrative(List.of(), "2025-10-01", "2025-10-31");

        assertThat(narrative)
                .isEqualTo("No festival or holiday correlated failures found between 2025-10-01 and 2025-10-31.");
    }

    @Test
    void festivalNarrative_whenRowsPresent() {
        List<AnalyticsOrderRepository.FestivalRow> rows = List.of(
                new AnalyticsOrderRepository.FestivalRow("Diwali", "Stockout", 4L, 0.85),
                new AnalyticsOrderRepository.FestivalRow("Diwali", "Traffic congestion", 2L, 0.85));

        String narrative = generator.festivalNarrative(rows, "2025-10-01", "2025-10-31");

        assertThat(narrative)
                .contains("Between 2025-10-01 and 2025-10-31, festival/holiday periods saw 6 failure events")
                .contains("Top correlated reason: Stockout")
                .contains("plan buffer staffing and driver capacity");
    }

    @Test
    void capacityNarrative_includesClientAndHighRiskCount() {
        CapacityRiskResponse.WarehouseRisk high = new CapacityRiskResponse.WarehouseRisk(
                1L, "WH-1", "Pune", 1000, 800, 950, 92.0, true);
        CapacityRiskResponse.WarehouseRisk low = new CapacityRiskResponse.WarehouseRisk(
                2L, "WH-2", "Mumbai", 1000, 500, 600, 60.0, false);
        CapacityRiskResponse result = new CapacityRiskResponse(
                10L, "BigBite", 5000, 0.12, List.of(high, low), null, List.of());

        String narrative = generator.capacityNarrative(result);

        assertThat(narrative)
                .contains("Client BigBite (id 10) historically fails 12.0% of orders")
                .contains("Adding 5,000 monthly orders")
                .contains("High-risk sites exceed 80% projected utilization");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "STOCKOUT_WAREHOUSE",
            "WAREHOUSE_OPS",
            "TRAFFIC_TRIPLE_CONFIRM",
            "ADDRESS_MISMATCH",
            "WEATHER_IMPACT",
            "SLA_BREACH",
            "FESTIVAL_VOLUME"
    })
    void recommendationsFromRules_producesRecommendationForKnownRule(String ruleId) {
        Map<String, String> context = Map.of("warehouseName", "WH-Pune-1");

        List<String> recommendations = generator.recommendationsFromRules(List.of(ruleId), context);

        assertThat(recommendations).hasSize(1).doesNotContainNull();
    }

    @Test
    void recommendationsFromRules_usesDefaultWarehouseNameWhenMissing() {
        List<String> recommendations = generator.recommendationsFromRules(
                List.of("WAREHOUSE_OPS"), Map.of());

        assertThat(recommendations).containsExactly("Add packing staff at the warehouse during peak hours.");
    }

    @Test
    void recommendationsFromRules_ignoresUnknownRuleIds() {
        List<String> recommendations = generator.recommendationsFromRules(
                List.of("UNKNOWN_RULE"), Map.of());

        assertThat(recommendations).isEmpty();
    }

    @Test
    void recommendationsFromRules_deduplicatesIdenticalRecommendations() {
        List<String> recommendations = generator.recommendationsFromRules(
                List.of("SLA_BREACH", "SLA_BREACH"), Map.of());

        assertThat(recommendations).hasSize(1);
    }

    @Test
    void capacityRecommendations_includesHighRiskWarehousesAndFailureRateWarning() {
        CapacityRiskResponse.WarehouseRisk high = new CapacityRiskResponse.WarehouseRisk(
                1L, "WH-Pune-1", "Pune", 1000, 800, 950, 92.0, true);
        CapacityRiskResponse result = new CapacityRiskResponse(
                10L, "BigBite", 5000, 0.20, List.of(high), null, List.of());

        List<String> recommendations = generator.capacityRecommendations(result);

        assertThat(recommendations)
                .contains("Increase capacity or reroute volume at WH-Pune-1 (Pune) — projected 92% utilization.")
                .contains("Historical failure rate exceeds 15% — run root-cause review before scaling order volume.")
                .contains("Add driver buffer proportional to projected order growth in primary client cities.");
    }

    @Test
    void buildEvidence_includesFailureReasonsRulesAndSentiment() {
        Map<String, Long> failureReasons = Map.of("Stockout", 3L);
        List<CorrelationRuleMatch> rules = List.of(
                new CorrelationRuleMatch("STOCKOUT_WAREHOUSE", 0.9),
                new CorrelationRuleMatch("STOCKOUT_WAREHOUSE", 0.8));
        Map<String, Long> sentiment = Map.of("negative", 2L);

        Map<String, Object> evidence = generator.buildEvidence(failureReasons, rules, sentiment);

        assertThat(evidence)
                .containsEntry("failureReasons", failureReasons)
                .containsEntry("correlatedRules", List.of("STOCKOUT_WAREHOUSE"))
                .containsEntry("feedbackSentiment", sentiment);
    }

    @Test
    void buildEvidence_omitsSentimentWhenNullOrEmpty() {
        Map<String, Object> evidenceNull = generator.buildEvidence(
                Map.of(), List.of(), null);
        Map<String, Object> evidenceEmpty = generator.buildEvidence(
                Map.of(), List.of(), Map.of());

        assertThat(evidenceNull).doesNotContainKey("feedbackSentiment");
        assertThat(evidenceEmpty).doesNotContainKey("feedbackSentiment");
    }
}
