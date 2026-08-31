package com.swifteats.analytics.controller;

import com.swifteats.analytics.dto.CapacityRiskResponse;
import com.swifteats.analytics.dto.CityComparisonResponse;
import com.swifteats.analytics.dto.DelayAnalysisResponse;
import com.swifteats.analytics.dto.FailureAnalysisResponse;
import com.swifteats.analytics.dto.InsightQueryRequest;
import com.swifteats.analytics.dto.InsightResponse;
import com.swifteats.analytics.dto.WarehouseFailureResponse;
import com.swifteats.analytics.service.AnalyticsQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AnalyticsControllerTest {

    @Mock
    private AnalyticsQueryService queryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AnalyticsController(queryService)).build();
    }

    @Test
    void delays_returnsAnalysis() throws Exception {
        when(queryService.analyzeDelaysByCity("Pune", LocalDate.parse("2025-03-17")))
                .thenReturn(new DelayAnalysisResponse(
                        "Pune", "2025-03-17", 3, Map.of("Delay", 2L), 1, 1, 0, "narrative", List.of("rec")));

        mockMvc.perform(get("/api/v1/analytics/delays")
                        .param("city", "Pune")
                        .param("date", "2025-03-17"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("Pune"));
    }

    @Test
    void failures_returnsAnalysis() throws Exception {
        when(queryService.analyzeFailuresByClient(eq(42L), any(Instant.class), any(Instant.class)))
                .thenReturn(new FailureAnalysisResponse(
                        42L, "2025-03-01", "2025-03-31", 2, List.of(), "n", List.of()));

        mockMvc.perform(get("/api/v1/analytics/failures")
                        .param("clientId", "42")
                        .param("from", "2025-03-01T00:00:00Z")
                        .param("to", "2025-03-31T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value(42));
    }

    @Test
    void failuresByWarehouse_usesMonthParam() throws Exception {
        when(queryService.analyzeFailuresByWarehouse(7L, 2025, 8))
                .thenReturn(new WarehouseFailureResponse(
                        7L, "WH", 8, 2025, 1, Map.of(), Map.of(), "n", List.of()));

        mockMvc.perform(get("/api/v1/analytics/failures/by-warehouse")
                        .param("warehouseId", "7")
                        .param("monthParam", "2025-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.warehouseId").value(7));
    }

    @Test
    void compareFailures_returnsComparison() throws Exception {
        when(queryService.compareCityFailures("Pune", "Mumbai", YearMonth.of(2025, 3)))
                .thenReturn(new CityComparisonResponse(
                        "Pune", "Mumbai", "2025-03", Map.of(), Map.of(), "n", List.of()));

        mockMvc.perform(get("/api/v1/analytics/failures/compare")
                        .param("cityA", "Pune")
                        .param("cityB", "Mumbai")
                        .param("year", "2025")
                        .param("month", "3"))
                .andExpect(status().isOk());
    }

    @Test
    void capacityProjection_returnsRisk() throws Exception {
        when(queryService.projectCapacityRisk(42L, 20000))
                .thenReturn(new CapacityRiskResponse(42L, "Acme", 20000, 0.2, List.of(), "n", List.of()));

        mockMvc.perform(get("/api/v1/analytics/capacity-projection")
                        .param("clientId", "42")
                        .param("additionalMonthlyOrders", "20000"))
                .andExpect(status().isOk());
    }

    @Test
    void insightQuery_acceptsPostBody() throws Exception {
        when(queryService.executeInsightQuery(any(InsightQueryRequest.class)))
                .thenReturn(new InsightResponse("n", List.of(), Map.of()));

        mockMvc.perform(post("/api/v1/analytics/insights/query")
                        .contentType("application/json")
                        .content("""
                                {"queryType":"FESTIVAL_ANALYSIS","parameters":{"from":"2025-03-01T00:00:00Z","to":"2025-03-31T00:00:00Z"}}
                                """))
                .andExpect(status().isOk());
    }
}
