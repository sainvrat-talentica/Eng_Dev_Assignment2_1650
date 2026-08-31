package com.swifteats.analytics.controller;

import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;

import com.swifteats.analytics.dto.CapacityRiskResponse;
import com.swifteats.analytics.dto.CityComparisonResponse;
import com.swifteats.analytics.dto.DelayAnalysisResponse;
import com.swifteats.analytics.dto.FailureAnalysisResponse;
import com.swifteats.analytics.dto.InsightQueryRequest;
import com.swifteats.analytics.dto.InsightResponse;
import com.swifteats.analytics.dto.WarehouseFailureResponse;
import com.swifteats.analytics.service.AnalyticsQueryService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;

@RestController
@RequestMapping("/api/v1/analytics")
@ServiceScope(ServiceName.ANALYTICS)
public class AnalyticsController {

    private final AnalyticsQueryService queryService;

    public AnalyticsController(AnalyticsQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/delays")
    public ResponseEntity<DelayAnalysisResponse> delays(
            @RequestParam String city,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(queryService.analyzeDelaysByCity(city, date));
    }

    @GetMapping("/failures")
    public ResponseEntity<FailureAnalysisResponse> failures(
            @RequestParam long clientId,
            @RequestParam Instant from,
            @RequestParam Instant to) {
        return ResponseEntity.ok(queryService.analyzeFailuresByClient(clientId, from, to));
    }

    @GetMapping("/failures/by-warehouse")
    public ResponseEntity<WarehouseFailureResponse> failuresByWarehouse(
            @RequestParam long warehouseId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) String monthParam) {
        YearMonth ym = resolveYearMonth(year, month, monthParam);
        return ResponseEntity.ok(queryService.analyzeFailuresByWarehouse(
                warehouseId, ym.getYear(), ym.getMonthValue()));
    }

    @GetMapping("/failures/compare")
    public ResponseEntity<CityComparisonResponse> compareFailures(
            @RequestParam String cityA,
            @RequestParam String cityB,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) String monthParam) {
        YearMonth ym = resolveYearMonth(year, month, monthParam);
        return ResponseEntity.ok(queryService.compareCityFailures(cityA, cityB, ym));
    }

    @GetMapping("/capacity-projection")
    public ResponseEntity<CapacityRiskResponse> capacityProjection(
            @RequestParam long clientId,
            @RequestParam int additionalMonthlyOrders) {
        return ResponseEntity.ok(queryService.projectCapacityRisk(clientId, additionalMonthlyOrders));
    }

    @PostMapping("/insights/query")
    public ResponseEntity<InsightResponse> insightQuery(@Valid @RequestBody InsightQueryRequest request) {
        return ResponseEntity.ok(queryService.executeInsightQuery(request));
    }

    private static YearMonth resolveYearMonth(Integer year, Integer month, String monthParam) {
        if (monthParam != null && !monthParam.isBlank()) {
            return YearMonth.parse(monthParam);
        }
        if (year == null || month == null) {
            throw new IllegalArgumentException("Provide month as yyyy-MM via monthParam or both year and month");
        }
        return YearMonth.of(year, month);
    }
}
