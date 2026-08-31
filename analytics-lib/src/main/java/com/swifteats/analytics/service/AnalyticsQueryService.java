package com.swifteats.analytics.service;

import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;

import com.swifteats.analytics.dto.CapacityRiskResponse;
import com.swifteats.analytics.dto.CityComparisonResponse;
import com.swifteats.analytics.dto.DelayAnalysisResponse;
import com.swifteats.analytics.dto.FailureAnalysisResponse;
import com.swifteats.analytics.dto.InsightQueryRequest;
import com.swifteats.analytics.dto.InsightResponse;
import com.swifteats.analytics.dto.WarehouseFailureResponse;
import com.swifteats.analytics.engine.CorrelationEngine;
import com.swifteats.analytics.engine.InsightGenerator;
import com.swifteats.analytics.exception.AnalyticsDataNotLoadedException;
import com.swifteats.analytics.model.CorrelationRuleMatch;
import com.swifteats.analytics.model.EnrichedOrder;
import com.swifteats.analytics.model.QueryType;
import com.swifteats.analytics.repository.AnalyticsOrderRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@ServiceScope(ServiceName.ANALYTICS)
public class AnalyticsQueryService {

    private static final int CORRELATION_SAMPLE_LIMIT = 200;

    private final AnalyticsOrderRepository repository;
    private final CorrelationEngine correlationEngine;
    private final InsightGenerator insightGenerator;

    public AnalyticsQueryService(
            AnalyticsOrderRepository repository,
            CorrelationEngine correlationEngine,
            InsightGenerator insightGenerator) {
        this.repository = repository;
        this.correlationEngine = correlationEngine;
        this.insightGenerator = insightGenerator;
    }

    public void ensureDataLoaded() {
        if (!repository.hasSampleData()) {
            throw new AnalyticsDataNotLoadedException();
        }
    }

    public DelayAnalysisResponse analyzeDelaysByCity(String city, LocalDate date) {
        ensureDataLoaded();
        int total = (int) repository.countAffectedByCityAndDate(city, date);
        Map<String, Long> reasons = repository.failureReasonCountsByCityAndDate(city, date);
        long heavyTraffic = repository.countHeavyTrafficByCityAndDate(city, date);
        long slowPacking = repository.countSlowPackingByCityAndDate(city, date);
        long negativeFeedback = repository.countNegativeFeedbackByCityAndDate(city, date);

        List<EnrichedOrder> sample = repository.findAffectedByCityAndDate(city, date, CORRELATION_SAMPLE_LIMIT);
        List<String> rules = collectRuleIds(sample);
        Map<String, String> context = Map.of("city", city);
        List<String> recommendations = insightGenerator.recommendationsFromRules(rules, context);

        DelayAnalysisResponse response = new DelayAnalysisResponse(
                city,
                date.toString(),
                total,
                reasons,
                heavyTraffic,
                slowPacking,
                negativeFeedback,
                "",
                recommendations);
        String narrative = insightGenerator.delayNarrative(response);
        return new DelayAnalysisResponse(
                city, date.toString(), total, reasons, heavyTraffic, slowPacking, negativeFeedback,
                narrative, recommendations);
    }

    public FailureAnalysisResponse analyzeFailuresByClient(long clientId, Instant from, Instant to) {
        ensureDataLoaded();
        int totalFailed = repository.countFailedByClient(clientId, from, to);
        String clientName = repository.findClientName(clientId);
        if (clientName == null) {
            clientName = "Client " + clientId;
        }

        List<FailureAnalysisResponse.FailureReasonBreakdown> breakdown = repository
                .failureBreakdownByClient(clientId, from, to)
                .stream()
                .map(row -> new FailureAnalysisResponse.FailureReasonBreakdown(
                        row.failureReason(),
                        row.count(),
                        row.fleetIssues(),
                        row.warehouseIssues()))
                .toList();

        List<EnrichedOrder> sample = repository.findFailedByClient(clientId, from, to, CORRELATION_SAMPLE_LIMIT);
        List<String> rules = collectRuleIds(sample);
        List<String> recommendations = insightGenerator.recommendationsFromRules(rules, Map.of());

        FailureAnalysisResponse draft = new FailureAnalysisResponse(
                clientId, from.toString(), to.toString(), totalFailed, breakdown, "", recommendations);
        String narrative = insightGenerator.clientFailureNarrative(draft, clientName);
        return new FailureAnalysisResponse(
                clientId, from.toString(), to.toString(), totalFailed, breakdown, narrative, recommendations);
    }

    public WarehouseFailureResponse analyzeFailuresByWarehouse(long warehouseId, int year, int month) {
        ensureDataLoaded();
        AnalyticsOrderRepository.WarehouseInfo warehouse = repository.findWarehouse(warehouseId);
        String warehouseName = warehouse != null ? warehouse.warehouseName() : "Warehouse " + warehouseId;

        int totalFailed = repository.countFailedByWarehouseAndMonth(warehouseId, year, month);
        Map<String, Long> reasons = repository.failureReasonCountsByWarehouseAndMonth(warehouseId, year, month);
        Map<String, Long> notes = repository.warehouseNoteCountsByWarehouseAndMonth(warehouseId, year, month);

        List<EnrichedOrder> sample = repository.findFailedByWarehouseAndMonth(
                warehouseId, year, month, CORRELATION_SAMPLE_LIMIT);
        List<String> rules = collectRuleIds(sample);
        Map<String, String> context = Map.of("warehouseName", warehouseName);
        List<String> recommendations = insightGenerator.recommendationsFromRules(rules, context);

        WarehouseFailureResponse draft = new WarehouseFailureResponse(
                warehouseId, warehouseName, month, year, totalFailed, reasons, notes, "", recommendations);
        String narrative = insightGenerator.warehouseFailureNarrative(draft);
        return new WarehouseFailureResponse(
                warehouseId, warehouseName, month, year, totalFailed, reasons, notes, narrative, recommendations);
    }

    public CityComparisonResponse compareCityFailures(String cityA, String cityB, YearMonth month) {
        ensureDataLoaded();
        Map<String, Long> failuresA = repository.failureReasonCountsByCityAndMonth(cityA, month);
        Map<String, Long> failuresB = repository.failureReasonCountsByCityAndMonth(cityB, month);

        List<String> rules = new ArrayList<>();
        rules.addAll(collectRuleIdsFromCounts(failuresA, cityA));
        rules.addAll(collectRuleIdsFromCounts(failuresB, cityB));
        List<String> recommendations = insightGenerator.recommendationsFromRules(rules.stream().distinct().toList(), Map.of());

        CityComparisonResponse draft = new CityComparisonResponse(
                cityA, cityB, month.toString(), failuresA, failuresB, "", recommendations);
        String narrative = insightGenerator.cityComparisonNarrative(draft);
        return new CityComparisonResponse(
                cityA, cityB, month.toString(), failuresA, failuresB, narrative, recommendations);
    }

    public InsightResponse analyzeFestivalPeriod(Instant from, Instant to) {
        ensureDataLoaded();
        List<AnalyticsOrderRepository.FestivalRow> rows = repository.festivalFailureAnalysis(from, to);
        List<EnrichedOrder> sample = repository.findFestivalOrders(from, to, CORRELATION_SAMPLE_LIMIT);
        List<String> rules = collectRuleIds(sample);
        if (rules.isEmpty()) {
            rules = List.of("FESTIVAL_VOLUME");
        }

        Map<String, Long> byReason = rows.stream()
                .filter(row -> row.failureReason() != null && !row.failureReason().isBlank())
                .collect(Collectors.groupingBy(
                        AnalyticsOrderRepository.FestivalRow::failureReason,
                        LinkedHashMap::new,
                        Collectors.summingLong(AnalyticsOrderRepository.FestivalRow::count)));

        String narrative = insightGenerator.festivalNarrative(rows, from.toString(), to.toString());
        List<String> recommendations = insightGenerator.recommendationsFromRules(rules, Map.of());

        Map<String, Object> evidence = insightGenerator.buildEvidence(byReason, toRuleMatches(rules), null);
        evidence.put("eventBreakdown", rows.stream()
                .collect(Collectors.groupingBy(
                        AnalyticsOrderRepository.FestivalRow::eventType,
                        Collectors.summingLong(AnalyticsOrderRepository.FestivalRow::count))));

        return new InsightResponse(narrative, recommendations, evidence);
    }

    public CapacityRiskResponse projectCapacityRisk(long clientId, int additionalMonthlyOrders) {
        ensureDataLoaded();
        String clientName = repository.findClientName(clientId);
        if (clientName == null) {
            clientName = "Client " + clientId;
        }

        double failureRate = repository.failureRateByClient(clientId);
        long totalOrders = repository.totalOrdersByClient(clientId);
        List<AnalyticsOrderRepository.WarehouseLoadRow> loads = repository.avgOrdersPerWarehouseForClient(clientId);

        List<CapacityRiskResponse.WarehouseRisk> risks = new ArrayList<>();
        for (AnalyticsOrderRepository.WarehouseLoadRow load : loads) {
            double share = totalOrders == 0 ? 0 : (double) load.orderCount() / totalOrders;
            long projectedExtra = Math.round(additionalMonthlyOrders * share);
            long projectedTotal = load.orderCount() + projectedExtra;
            double utilization = load.capacity() == 0 ? 100.0 : projectedTotal * 100.0 / load.capacity();
            risks.add(new CapacityRiskResponse.WarehouseRisk(
                    load.warehouseId(),
                    load.warehouseName(),
                    load.city(),
                    load.capacity(),
                    load.orderCount(),
                    projectedTotal,
                    utilization,
                    utilization >= 80.0));
        }

        CapacityRiskResponse draft = new CapacityRiskResponse(
                clientId, clientName, additionalMonthlyOrders, failureRate, risks, "", List.of());
        String narrative = insightGenerator.capacityNarrative(draft);
        List<String> recommendations = insightGenerator.capacityRecommendations(draft);
        return new CapacityRiskResponse(
                clientId, clientName, additionalMonthlyOrders, failureRate, risks, narrative, recommendations);
    }

    public InsightResponse executeInsightQuery(InsightQueryRequest request) {
        Map<String, String> params = request.parameters();
        return switch (request.queryType()) {
            case DELAY_BY_CITY -> toInsight(analyzeDelaysByCity(
                    require(params, "city"),
                    parseDate(require(params, "date"))));
            case FAILURES_BY_CLIENT -> toInsight(analyzeFailuresByClient(
                    Long.parseLong(require(params, "clientId")),
                    parseInstant(require(params, "from")),
                    parseInstant(require(params, "to"))));
            case FAILURES_BY_WAREHOUSE -> {
                YearMonth ym = parseYearMonth(params);
                yield toInsight(analyzeFailuresByWarehouse(
                        Long.parseLong(require(params, "warehouseId")),
                        ym.getYear(),
                        ym.getMonthValue()));
            }
            case COMPARE_CITIES -> toInsight(compareCityFailures(
                    require(params, "cityA"),
                    require(params, "cityB"),
                    parseYearMonth(params)));
            case FESTIVAL_ANALYSIS -> analyzeFestivalPeriod(
                    parseInstant(require(params, "from")),
                    parseInstant(require(params, "to")));
            case CAPACITY_PROJECTION -> toInsight(projectCapacityRisk(
                    Long.parseLong(require(params, "clientId")),
                    Integer.parseInt(require(params, "additionalMonthlyOrders"))));
        };
    }

    private InsightResponse toInsight(DelayAnalysisResponse result) {
        Map<String, Object> evidence = insightGenerator.buildEvidence(
                result.failureReasonCounts(),
                List.of(),
                Map.of("Negative", result.negativeFeedbackCount()));
        return new InsightResponse(result.narrative(), result.recommendations(), evidence);
    }

    private InsightResponse toInsight(FailureAnalysisResponse result) {
        Map<String, Long> reasons = result.breakdown().stream()
                .collect(Collectors.toMap(
                        FailureAnalysisResponse.FailureReasonBreakdown::failureReason,
                        FailureAnalysisResponse.FailureReasonBreakdown::count,
                        Long::sum,
                        LinkedHashMap::new));
        return new InsightResponse(
                result.narrative(),
                result.recommendations(),
                insightGenerator.buildEvidence(reasons, List.of(), null));
    }

    private InsightResponse toInsight(WarehouseFailureResponse result) {
        return new InsightResponse(
                result.narrative(),
                result.recommendations(),
                insightGenerator.buildEvidence(result.failureReasonCounts(), List.of(), null));
    }

    private InsightResponse toInsight(CityComparisonResponse result) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("cityA", result.cityAFailures());
        evidence.put("cityB", result.cityBFailures());
        return new InsightResponse(result.narrative(), result.recommendations(), evidence);
    }

    private InsightResponse toInsight(CapacityRiskResponse result) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("historicalFailureRate", result.historicalFailureRate());
        evidence.put("warehouseRisks", result.warehouseRisks());
        return new InsightResponse(result.narrative(), result.recommendations(), evidence);
    }

    private List<String> collectRuleIds(List<EnrichedOrder> orders) {
        return orders.stream()
                .flatMap(o -> correlationEngine.correlate(o).matches().stream())
                .map(CorrelationRuleMatch::ruleId)
                .distinct()
                .toList();
    }

    private List<String> collectRuleIdsFromCounts(Map<String, Long> failures, String city) {
        if (failures.containsKey("Incorrect address")) {
            return List.of("ADDRESS_MISMATCH");
        }
        if (failures.containsKey("Traffic congestion")) {
            return List.of("TRAFFIC_TRIPLE_CONFIRM");
        }
        if (failures.containsKey("Warehouse delay")) {
            return List.of("WAREHOUSE_OPS");
        }
        return List.of();
    }

    private List<CorrelationRuleMatch> toRuleMatches(List<String> ruleIds) {
        return ruleIds.stream()
                .map(id -> new CorrelationRuleMatch(id, 0.75))
                .toList();
    }

    private static String require(Map<String, String> params, String key) {
        String value = params.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required parameter: " + key);
        }
        return value.trim();
    }

    private static LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid date format (expected yyyy-MM-dd): " + value);
        }
    }

    private static Instant parseInstant(String value) {
        try {
            if (value.length() == 10) {
                return LocalDate.parse(value).atStartOfDay().toInstant(ZoneOffset.UTC);
            }
            return Instant.parse(value);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid instant format: " + value);
        }
    }

    private static YearMonth parseYearMonth(Map<String, String> params) {
        if (params.containsKey("month") && params.get("month").contains("-")) {
            return YearMonth.parse(params.get("month").trim());
        }
        int year = Integer.parseInt(require(params, "year"));
        int month = Integer.parseInt(require(params, "month"));
        return YearMonth.of(year, month);
    }
}
