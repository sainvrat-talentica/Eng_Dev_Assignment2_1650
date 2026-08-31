package com.swifteats.analytics.engine;

import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;

import com.swifteats.analytics.dto.CapacityRiskResponse;
import com.swifteats.analytics.dto.CityComparisonResponse;
import com.swifteats.analytics.dto.DelayAnalysisResponse;
import com.swifteats.analytics.dto.FailureAnalysisResponse;
import com.swifteats.analytics.dto.WarehouseFailureResponse;
import com.swifteats.analytics.model.CorrelationRuleMatch;
import com.swifteats.analytics.repository.AnalyticsOrderRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@ServiceScope(ServiceName.ANALYTICS)
public class InsightGenerator {

    public String delayNarrative(DelayAnalysisResponse result) {
        String topReason = topKey(result.failureReasonCounts());
        double topPct = pct(result.failureReasonCounts(), topReason, result.totalAffected());

        return String.format(
                Locale.US,
                "In %s on %s, %d deliveries were delayed or failed. "
                        + "Top cause: %s (%.0f%% of affected orders). "
                        + "%d orders correlated with heavy traffic and %d with slow warehouse packing. "
                        + "%d customers left negative feedback mentioning lateness or service issues.",
                result.city(),
                result.date(),
                result.totalAffected(),
                topReason,
                topPct,
                result.heavyTrafficCount(),
                result.slowPackingCount(),
                result.negativeFeedbackCount());
    }

    public String clientFailureNarrative(FailureAnalysisResponse result, String clientName) {
        if (result.totalFailed() == 0) {
            return String.format(
                    Locale.US,
                    "Client %s (id %d) had no failed orders between %s and %s.",
                    clientName, result.clientId(), result.from(), result.to());
        }
        var top = result.breakdown().get(0);
        return String.format(
                Locale.US,
                "Client %s (id %d) recorded %d failed orders from %s to %s. "
                        + "Primary failure reason: %s (%d orders). "
                        + "Fleet issues noted: %s. Warehouse issues: %s.",
                clientName,
                result.clientId(),
                result.totalFailed(),
                result.from(),
                result.to(),
                top.failureReason(),
                top.count(),
                nullToDash(top.fleetIssues()),
                nullToDash(top.warehouseIssues()));
    }

    public String warehouseFailureNarrative(WarehouseFailureResponse result) {
        String topReason = topKey(result.failureReasonCounts());
        return String.format(
                Locale.US,
                "Warehouse %s (id %d) had %d failed orders in %d-%02d. "
                        + "Leading failure reason: %s. "
                        + "Warehouse log patterns: %s.",
                result.warehouseName(),
                result.warehouseId(),
                result.totalFailed(),
                result.year(),
                result.month(),
                topReason,
                summarizeCounts(result.warehouseNoteCounts()));
    }

    public String cityComparisonNarrative(CityComparisonResponse result) {
        String topA = topKey(result.cityAFailures());
        String topB = topKey(result.cityBFailures());
        long totalA = result.cityAFailures().values().stream().mapToLong(Long::longValue).sum();
        long totalB = result.cityBFailures().values().stream().mapToLong(Long::longValue).sum();
        return String.format(
                Locale.US,
                "During %s, %s had %d failed deliveries (top cause: %s) "
                        + "while %s had %d (top cause: %s). "
                        + "Compare operational focus: address verification in cities with high incorrect-address rates, "
                        + "and warehouse staffing where warehouse-delay failures dominate.",
                result.month(),
                result.cityA(),
                totalA,
                topA,
                result.cityB(),
                totalB,
                topB);
    }

    public String festivalNarrative(List<AnalyticsOrderRepository.FestivalRow> rows, String from, String to) {
        if (rows.isEmpty()) {
            return String.format(
                    Locale.US,
                    "No festival or holiday correlated failures found between %s and %s.",
                    from, to);
        }
        Map<String, Long> byReason = rows.stream()
                .filter(row -> row.failureReason() != null && !row.failureReason().isBlank())
                .collect(Collectors.groupingBy(
                        AnalyticsOrderRepository.FestivalRow::failureReason,
                        Collectors.summingLong(AnalyticsOrderRepository.FestivalRow::count)));
        String topReason = topKey(byReason);
        long total = byReason.values().stream().mapToLong(Long::longValue).sum();
        return String.format(
                Locale.US,
                "Between %s and %s, festival/holiday periods saw %d failure events. "
                        + "Top correlated reason: %s. "
                        + "Average warehouse capacity during these events was constrained — plan buffer staffing and driver capacity.",
                from, to, total, topReason);
    }

    public String capacityNarrative(CapacityRiskResponse result) {
        long highRisk = result.warehouseRisks().stream().filter(CapacityRiskResponse.WarehouseRisk::highRisk).count();
        return String.format(
                Locale.US,
                "Client %s (id %d) historically fails %.1f%% of orders. "
                        + "Adding %,d monthly orders projects strain on %d warehouse(s). "
                        + "High-risk sites exceed 80%% projected utilization and need mitigation before onboarding.",
                result.clientName(),
                result.clientId(),
                result.historicalFailureRate() * 100,
                result.additionalMonthlyOrders(),
                highRisk);
    }

    public List<String> recommendationsFromRules(List<String> ruleIds, Map<String, String> context) {
        List<String> recommendations = new ArrayList<>();
        for (String ruleId : ruleIds) {
            switch (ruleId) {
                case "STOCKOUT_WAREHOUSE" ->
                        recommendations.add("Enable real-time inventory sync and low-stock alerts at affected warehouses.");
                case "WAREHOUSE_OPS" -> {
                    String warehouse = context.getOrDefault("warehouseName", "the warehouse");
                    recommendations.add("Add packing staff at " + warehouse + " during peak hours.");
                }
                case "TRAFFIC_TRIPLE_CONFIRM" ->
                        recommendations.add("Reroute via alternate routes and widen ETA windows during heavy traffic.");
                case "ADDRESS_MISMATCH" ->
                        recommendations.add("Mandatory address pin validation before dispatch.");
                case "WEATHER_IMPACT" ->
                        recommendations.add("Pause SLA clock during severe weather and pre-position drivers beforehand.");
                case "SLA_BREACH" ->
                        recommendations.add("Review end-to-end SLA checkpoints from kitchen dispatch to last-mile handoff.");
                case "FESTIVAL_VOLUME" ->
                        recommendations.add("Pre-scale warehouse capacity by 15% and add a 15% driver buffer before festival peaks.");
                default -> {
                }
            }
        }
        return recommendations.stream().distinct().toList();
    }

    public List<String> capacityRecommendations(CapacityRiskResponse result) {
        List<String> recs = new ArrayList<>();
        for (CapacityRiskResponse.WarehouseRisk risk : result.warehouseRisks()) {
            if (risk.highRisk()) {
                recs.add(String.format(
                        Locale.US,
                        "Increase capacity or reroute volume at %s (%s) — projected %.0f%% utilization.",
                        risk.warehouseName(), risk.city(), risk.projectedUtilizationPct()));
            }
        }
        if (result.historicalFailureRate() > 0.15) {
            recs.add("Historical failure rate exceeds 15% — run root-cause review before scaling order volume.");
        }
        recs.add("Add driver buffer proportional to projected order growth in primary client cities.");
        return recs.stream().distinct().toList();
    }

    public Map<String, Object> buildEvidence(
            Map<String, Long> failureReasons,
            List<CorrelationRuleMatch> rules,
            Map<String, Long> sentimentCounts) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("failureReasons", failureReasons);
        evidence.put("correlatedRules", rules.stream().map(CorrelationRuleMatch::ruleId).distinct().toList());
        if (sentimentCounts != null && !sentimentCounts.isEmpty()) {
            evidence.put("feedbackSentiment", sentimentCounts);
        }
        return evidence;
    }

    private static String topKey(Map<String, Long> counts) {
        return counts.isEmpty() ? "None" : counts.keySet().iterator().next();
    }

    private static double pct(Map<String, Long> counts, String key, int total) {
        if (total == 0 || key == null) {
            return 0;
        }
        return counts.getOrDefault(key, 0L) * 100.0 / total;
    }

    private static String nullToDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private static String summarizeCounts(Map<String, Long> counts) {
        if (counts.isEmpty()) {
            return "none recorded";
        }
        return counts.entrySet().stream()
                .limit(3)
                .map(e -> e.getKey() + " (" + e.getValue() + ")")
                .collect(Collectors.joining(", "));
    }
}
