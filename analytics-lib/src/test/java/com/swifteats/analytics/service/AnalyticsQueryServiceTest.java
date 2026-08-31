package com.swifteats.analytics.service;

import com.swifteats.analytics.dto.CapacityRiskResponse;
import com.swifteats.analytics.dto.CityComparisonResponse;
import com.swifteats.analytics.dto.FailureAnalysisResponse;
import com.swifteats.analytics.dto.InsightQueryRequest;
import com.swifteats.analytics.dto.WarehouseFailureResponse;
import com.swifteats.analytics.engine.CorrelationEngine;
import com.swifteats.analytics.engine.InsightGenerator;
import com.swifteats.analytics.exception.AnalyticsDataNotLoadedException;
import com.swifteats.analytics.model.QueryType;
import com.swifteats.analytics.repository.AnalyticsOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsQueryServiceTest {

    private static final LocalDate DATE = LocalDate.parse("2025-03-17");
    private static final Instant FROM = Instant.parse("2025-03-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2025-03-31T00:00:00Z");

    @Mock
    private AnalyticsOrderRepository repository;

    private AnalyticsQueryService service;

    @BeforeEach
    void setUp() {
        service = new AnalyticsQueryService(
                repository,
                new CorrelationEngine(),
                new InsightGenerator());
    }

    @Test
    void throwsWhenSampleDataMissing() {
        when(repository.hasSampleData()).thenReturn(false);

        assertThatThrownBy(() -> service.analyzeDelaysByCity("Pune", DATE))
                .isInstanceOf(AnalyticsDataNotLoadedException.class);
    }

    @Test
    void analyzeDelaysByCity_buildsInsightWithNarrative() {
        stubDelayByCityData();

        var response = service.analyzeDelaysByCity("Pune", DATE);

        assertThat(response.narrative()).contains("Pune");
        assertThat(response.totalAffected()).isEqualTo(10);
        assertThat(response.failureReasonCounts()).containsKey("Warehouse delay");
    }

    @Test
    void analyzeFailuresByClient_buildsBreakdownAndNarrative() {
        when(repository.hasSampleData()).thenReturn(true);
        when(repository.countFailedByClient(42L, FROM, TO)).thenReturn(3);
        when(repository.findClientName(42L)).thenReturn("Acme Corp");
        when(repository.failureBreakdownByClient(42L, FROM, TO)).thenReturn(List.of(
                new AnalyticsOrderRepository.FailureReasonRow("Warehouse delay", 2L, "GPS lag", "Slow packing"),
                new AnalyticsOrderRepository.FailureReasonRow("Traffic congestion", 1L, null, null)));
        when(repository.findFailedByClient(eq(42L), eq(FROM), eq(TO), anyInt())).thenReturn(List.of());

        FailureAnalysisResponse response = service.analyzeFailuresByClient(42L, FROM, TO);

        assertThat(response.totalFailed()).isEqualTo(3);
        assertThat(response.breakdown()).hasSize(2);
        assertThat(response.narrative()).contains("Acme Corp");
    }

    @Test
    void analyzeFailuresByClient_usesFallbackClientName() {
        when(repository.hasSampleData()).thenReturn(true);
        when(repository.countFailedByClient(99L, FROM, TO)).thenReturn(0);
        when(repository.findClientName(99L)).thenReturn(null);
        when(repository.failureBreakdownByClient(99L, FROM, TO)).thenReturn(List.of());
        when(repository.findFailedByClient(eq(99L), any(), any(), anyInt())).thenReturn(List.of());

        FailureAnalysisResponse response = service.analyzeFailuresByClient(99L, FROM, TO);

        assertThat(response.narrative()).contains("Client 99");
    }

    @Test
    void analyzeFailuresByWarehouse_buildsWarehouseInsight() {
        when(repository.hasSampleData()).thenReturn(true);
        when(repository.findWarehouse(7L)).thenReturn(
                new AnalyticsOrderRepository.WarehouseInfo(7L, "Pune Central", "Pune", 500));
        when(repository.countFailedByWarehouseAndMonth(7L, 2025, 3)).thenReturn(5);
        when(repository.failureReasonCountsByWarehouseAndMonth(7L, 2025, 3))
                .thenReturn(new LinkedHashMap<>(Map.of("Warehouse delay", 3L)));
        when(repository.warehouseNoteCountsByWarehouseAndMonth(7L, 2025, 3))
                .thenReturn(new LinkedHashMap<>(Map.of("Slow packing", 2L)));
        when(repository.findFailedByWarehouseAndMonth(eq(7L), eq(2025), eq(3), anyInt())).thenReturn(List.of());

        WarehouseFailureResponse response = service.analyzeFailuresByWarehouse(7L, 2025, 3);

        assertThat(response.warehouseName()).isEqualTo("Pune Central");
        assertThat(response.totalFailed()).isEqualTo(5);
        assertThat(response.narrative()).isNotBlank();
    }

    @Test
    void compareCityFailures_buildsComparisonInsight() {
        when(repository.hasSampleData()).thenReturn(true);
        when(repository.failureReasonCountsByCityAndMonth("Pune", YearMonth.of(2025, 3)))
                .thenReturn(new LinkedHashMap<>(Map.of("Traffic congestion", 4L)));
        when(repository.failureReasonCountsByCityAndMonth("Mumbai", YearMonth.of(2025, 3)))
                .thenReturn(new LinkedHashMap<>(Map.of("Incorrect address", 2L)));

        CityComparisonResponse response = service.compareCityFailures("Pune", "Mumbai", YearMonth.of(2025, 3));

        assertThat(response.cityAFailures()).containsKey("Traffic congestion");
        assertThat(response.cityBFailures()).containsKey("Incorrect address");
        assertThat(response.narrative()).isNotBlank();
    }

    @Test
    void analyzeFestivalPeriod_buildsFestivalInsight() {
        when(repository.hasSampleData()).thenReturn(true);
        when(repository.festivalFailureAnalysis(FROM, TO)).thenReturn(List.of(
                new AnalyticsOrderRepository.FestivalRow("Festival", "Warehouse delay", 8L, 400.0),
                new AnalyticsOrderRepository.FestivalRow("Holiday", "Traffic congestion", 3L, 350.0)));
        when(repository.findFestivalOrders(eq(FROM), eq(TO), anyInt())).thenReturn(List.of());

        var response = service.analyzeFestivalPeriod(FROM, TO);

        assertThat(response.narrative()).isNotBlank();
        assertThat(response.evidence()).containsKey("eventBreakdown");
        assertThat(response.evidence()).containsKey("failureReasons");
    }

    @Test
    void projectCapacityRisk_flagsHighUtilization() {
        when(repository.hasSampleData()).thenReturn(true);
        when(repository.findClientName(10L)).thenReturn("Big Client");
        when(repository.failureRateByClient(10L)).thenReturn(0.05);
        when(repository.totalOrdersByClient(10L)).thenReturn(100L);
        when(repository.avgOrdersPerWarehouseForClient(10L)).thenReturn(List.of(
                new AnalyticsOrderRepository.WarehouseLoadRow(1L, "WH-A", "Pune", 100, 80L),
                new AnalyticsOrderRepository.WarehouseLoadRow(2L, "WH-B", "Mumbai", 200, 50L)));

        CapacityRiskResponse response = service.projectCapacityRisk(10L, 50);

        assertThat(response.clientName()).isEqualTo("Big Client");
        assertThat(response.warehouseRisks()).hasSize(2);
        assertThat(response.warehouseRisks().stream().anyMatch(CapacityRiskResponse.WarehouseRisk::highRisk)).isTrue();
        assertThat(response.narrative()).isNotBlank();
    }

    @Test
    void executeInsightQuery_delayByCity() {
        stubDelayByCityData();

        var response = service.executeInsightQuery(new InsightQueryRequest(
                QueryType.DELAY_BY_CITY,
                Map.of("city", "Pune", "date", "2025-03-17")));

        assertThat(response.narrative()).contains("Pune");
        assertThat(response.evidence()).containsKey("failureReasons");
    }

    @Test
    void executeInsightQuery_failuresByClient() {
        when(repository.hasSampleData()).thenReturn(true);
        when(repository.countFailedByClient(42L, FROM, TO)).thenReturn(1);
        when(repository.findClientName(42L)).thenReturn("Acme");
        when(repository.failureBreakdownByClient(42L, FROM, TO)).thenReturn(List.of(
                new AnalyticsOrderRepository.FailureReasonRow("Warehouse delay", 1L, null, null)));
        when(repository.findFailedByClient(eq(42L), any(), any(), anyInt())).thenReturn(List.of());

        var response = service.executeInsightQuery(new InsightQueryRequest(
                QueryType.FAILURES_BY_CLIENT,
                Map.of("clientId", "42", "from", "2025-03-01", "to", "2025-03-31")));

        assertThat(response.narrative()).contains("Acme");
    }

    @Test
    void executeInsightQuery_failuresByWarehouse_withYearMonthString() {
        when(repository.hasSampleData()).thenReturn(true);
        when(repository.findWarehouse(7L)).thenReturn(
                new AnalyticsOrderRepository.WarehouseInfo(7L, "WH-7", "Pune", 100));
        when(repository.countFailedByWarehouseAndMonth(7L, 2025, 3)).thenReturn(2);
        when(repository.failureReasonCountsByWarehouseAndMonth(7L, 2025, 3)).thenReturn(Map.of());
        when(repository.warehouseNoteCountsByWarehouseAndMonth(7L, 2025, 3)).thenReturn(Map.of());
        when(repository.findFailedByWarehouseAndMonth(eq(7L), eq(2025), eq(3), anyInt())).thenReturn(List.of());

        var response = service.executeInsightQuery(new InsightQueryRequest(
                QueryType.FAILURES_BY_WAREHOUSE,
                Map.of("warehouseId", "7", "month", "2025-03")));

        assertThat(response.narrative()).isNotBlank();
    }

    @Test
    void executeInsightQuery_compareCities() {
        when(repository.hasSampleData()).thenReturn(true);
        when(repository.failureReasonCountsByCityAndMonth("Pune", YearMonth.of(2025, 3))).thenReturn(Map.of());
        when(repository.failureReasonCountsByCityAndMonth("Mumbai", YearMonth.of(2025, 3))).thenReturn(Map.of());

        var response = service.executeInsightQuery(new InsightQueryRequest(
                QueryType.COMPARE_CITIES,
                Map.of("cityA", "Pune", "cityB", "Mumbai", "year", "2025", "month", "3")));

        assertThat(response.evidence()).containsKeys("cityA", "cityB");
    }

    @Test
    void executeInsightQuery_festivalAnalysis() {
        when(repository.hasSampleData()).thenReturn(true);
        when(repository.festivalFailureAnalysis(FROM, TO)).thenReturn(List.of());
        when(repository.findFestivalOrders(any(), any(), anyInt())).thenReturn(List.of());

        var response = service.executeInsightQuery(new InsightQueryRequest(
                QueryType.FESTIVAL_ANALYSIS,
                Map.of("from", "2025-03-01T00:00:00Z", "to", "2025-03-31T00:00:00Z")));

        assertThat(response.evidence()).containsKey("eventBreakdown");
    }

    @Test
    void executeInsightQuery_capacityProjection() {
        when(repository.hasSampleData()).thenReturn(true);
        when(repository.findClientName(10L)).thenReturn("Client");
        when(repository.failureRateByClient(10L)).thenReturn(0.0);
        when(repository.totalOrdersByClient(10L)).thenReturn(0L);
        when(repository.avgOrdersPerWarehouseForClient(10L)).thenReturn(List.of());

        var response = service.executeInsightQuery(new InsightQueryRequest(
                QueryType.CAPACITY_PROJECTION,
                Map.of("clientId", "10", "additionalMonthlyOrders", "100")));

        assertThat(response.evidence()).containsKey("warehouseRisks");
    }

    @ParameterizedTest
    @EnumSource(QueryType.class)
    void executeInsightQuery_throwsWhenDataMissing(QueryType queryType) {
        when(repository.hasSampleData()).thenReturn(false);

        assertThatThrownBy(() -> service.executeInsightQuery(sampleRequestFor(queryType)))
                .isInstanceOf(AnalyticsDataNotLoadedException.class);
    }

    @Test
    void executeInsightQuery_rejectsInvalidDate() {
        assertThatThrownBy(() -> service.executeInsightQuery(new InsightQueryRequest(
                QueryType.DELAY_BY_CITY,
                Map.of("city", "Pune", "date", "not-a-date"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid date format");
    }

    @Test
    void executeInsightQuery_rejectsInvalidInstant() {
        assertThatThrownBy(() -> service.executeInsightQuery(new InsightQueryRequest(
                QueryType.FAILURES_BY_CLIENT,
                Map.of("clientId", "1", "from", "bad-instant", "to", "2025-03-31"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid instant format");
    }

    @Test
    void executeInsightQuery_rejectsMissingRequiredParameter() {
        assertThatThrownBy(() -> service.executeInsightQuery(new InsightQueryRequest(
                QueryType.DELAY_BY_CITY,
                Map.of("city", "Pune"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing required parameter: date");
    }

    private void stubDelayByCityData() {
        when(repository.hasSampleData()).thenReturn(true);
        when(repository.countAffectedByCityAndDate("Pune", DATE)).thenReturn(10L);
        when(repository.failureReasonCountsByCityAndDate(eq("Pune"), any())).thenReturn(
                new LinkedHashMap<>(Map.of("Warehouse delay", 5L, "Traffic congestion", 3L)));
        when(repository.countHeavyTrafficByCityAndDate(any(), any())).thenReturn(3L);
        when(repository.countSlowPackingByCityAndDate(any(), any())).thenReturn(2L);
        when(repository.countNegativeFeedbackByCityAndDate(any(), any())).thenReturn(4L);
        when(repository.findAffectedByCityAndDate(any(), any(), anyInt())).thenReturn(List.of());
    }

    private static InsightQueryRequest sampleRequestFor(QueryType queryType) {
        return switch (queryType) {
            case DELAY_BY_CITY -> new InsightQueryRequest(
                    QueryType.DELAY_BY_CITY, Map.of("city", "Pune", "date", "2025-03-17"));
            case FAILURES_BY_CLIENT -> new InsightQueryRequest(
                    QueryType.FAILURES_BY_CLIENT,
                    Map.of("clientId", "1", "from", "2025-03-01", "to", "2025-03-31"));
            case FAILURES_BY_WAREHOUSE -> new InsightQueryRequest(
                    QueryType.FAILURES_BY_WAREHOUSE, Map.of("warehouseId", "1", "month", "2025-03"));
            case COMPARE_CITIES -> new InsightQueryRequest(
                    QueryType.COMPARE_CITIES,
                    Map.of("cityA", "Pune", "cityB", "Mumbai", "year", "2025", "month", "3"));
            case FESTIVAL_ANALYSIS -> new InsightQueryRequest(
                    QueryType.FESTIVAL_ANALYSIS,
                    Map.of("from", "2025-03-01", "to", "2025-03-31"));
            case CAPACITY_PROJECTION -> new InsightQueryRequest(
                    QueryType.CAPACITY_PROJECTION,
                    Map.of("clientId", "1", "additionalMonthlyOrders", "10"));
        };
    }
}
