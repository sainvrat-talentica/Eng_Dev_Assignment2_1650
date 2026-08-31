package com.swifteats.analytics.repository;

import com.swifteats.analytics.model.EnrichedOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsOrderRepositoryTest {

    private static final LocalDate DATE = LocalDate.parse("2025-03-17");
    private static final Instant FROM = Instant.parse("2025-03-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2025-03-31T00:00:00Z");

    @Mock
    private JdbcTemplate jdbcTemplate;

    private AnalyticsOrderRepository repository;

    @BeforeEach
    void setUp() {
        repository = new AnalyticsOrderRepository(jdbcTemplate);
        lenient().when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(), any())).thenReturn(1L);
        lenient().when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any())).thenReturn(1L);
        lenient().when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(), any(), any())).thenReturn(1);
        lenient().when(jdbcTemplate.queryForObject(anyString(), eq(Double.class), any())).thenReturn(0.1);
        lenient().when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(), any(), any())).thenReturn(List.of());
        lenient().when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(), any())).thenReturn(List.of());
        lenient().when(jdbcTemplate.query(anyString(), any(RowMapper.class), any())).thenReturn(List.of());
    }

    @Test
    void hasSampleData_trueWhenCountPositive() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(10L);
        assertThat(repository.hasSampleData()).isTrue();
    }

    @Test
    void countAffectedByCityAndDate_returnsCount() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq("Pune"), eq(DATE))).thenReturn(5L);
        assertThat(repository.countAffectedByCityAndDate("Pune", DATE)).isEqualTo(5L);
    }

    @Test
    void failureReasonCountsByCityAndDate_returnsMap() throws Exception {
        when(jdbcTemplate.query(anyString(), any(org.springframework.jdbc.core.ResultSetExtractor.class), eq("Pune"), eq(DATE)))
                .thenAnswer(invocation -> {
                    ResultSet rs = mock(ResultSet.class);
                    when(rs.next()).thenReturn(true, false);
                    when(rs.getString("reason")).thenReturn("Warehouse delay");
                    when(rs.getLong("cnt")).thenReturn(3L);
                    @SuppressWarnings("unchecked")
                    org.springframework.jdbc.core.ResultSetExtractor<Map<String, Long>> extractor =
                            invocation.getArgument(1);
                    return extractor.extractData(rs);
                });

        assertThat(repository.failureReasonCountsByCityAndDate("Pune", DATE))
                .containsEntry("Warehouse delay", 3L);
    }

    @Test
    void countHeavyTrafficByCityAndDate_delegatesToJdbc() {
        assertThat(repository.countHeavyTrafficByCityAndDate("Pune", DATE)).isEqualTo(1L);
    }

    @Test
    void countSlowPackingByCityAndDate_delegatesToJdbc() {
        assertThat(repository.countSlowPackingByCityAndDate("Pune", DATE)).isEqualTo(1L);
    }

    @Test
    void countNegativeFeedbackByCityAndDate_delegatesToJdbc() {
        assertThat(repository.countNegativeFeedbackByCityAndDate("Pune", DATE)).isEqualTo(1L);
    }

    @Test
    void findClientName_returnsNameWhenPresent() throws Exception {
        when(jdbcTemplate.query(anyString(), any(org.springframework.jdbc.core.ResultSetExtractor.class), eq(42L)))
                .thenAnswer(invocation -> {
                    ResultSet rs = mock(ResultSet.class);
                    when(rs.next()).thenReturn(true);
                    when(rs.getString("client_name")).thenReturn("Acme Corp");
                    @SuppressWarnings("unchecked")
                    org.springframework.jdbc.core.ResultSetExtractor<String> extractor = invocation.getArgument(1);
                    return extractor.extractData(rs);
                });

        assertThat(repository.findClientName(42L)).isEqualTo("Acme Corp");
    }

    @Test
    void findAffectedByCityAndDate_returnsMappedOrders() {
        EnrichedOrder order = sampleOrder();
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq("Pune"), eq(DATE), eq(50)))
                .thenReturn(List.of(order));

        assertThat(repository.findAffectedByCityAndDate("Pune", DATE, 50)).containsExactly(order);
    }

    @Test
    void failureBreakdownByClient_returnsRows() {
        var row = new AnalyticsOrderRepository.FailureReasonRow("Delay", 2L, "GPS", "Slow packing");
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(42L), any(), any()))
                .thenReturn(List.of(row));

        assertThat(repository.failureBreakdownByClient(42L, FROM, TO)).containsExactly(row);
    }

    @Test
    void countFailedByClient_returnsCount() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(42L), any(), any())).thenReturn(7L);
        assertThat(repository.countFailedByClient(42L, FROM, TO)).isEqualTo(7);
    }

    @Test
    void findWarehouse_returnsInfo() throws Exception {
        when(jdbcTemplate.query(anyString(), any(org.springframework.jdbc.core.ResultSetExtractor.class), eq(9L)))
                .thenAnswer(invocation -> {
                    ResultSet rs = mock(ResultSet.class);
                    when(rs.next()).thenReturn(true);
                    when(rs.getLong("warehouse_id")).thenReturn(9L);
                    when(rs.getString("warehouse_name")).thenReturn("WH-B");
                    when(rs.getString("city")).thenReturn("Pune");
                    when(rs.getInt("capacity")).thenReturn(100);
                    @SuppressWarnings("unchecked")
                    org.springframework.jdbc.core.ResultSetExtractor<AnalyticsOrderRepository.WarehouseInfo> extractor =
                            invocation.getArgument(1);
                    return extractor.extractData(rs);
                });

        AnalyticsOrderRepository.WarehouseInfo info = repository.findWarehouse(9L);
        assertThat(info.warehouseName()).isEqualTo("WH-B");
    }

    @Test
    void warehouseAndFestivalQueries_execute() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(1L), eq(2025), eq(8))).thenReturn(1L);
        when(jdbcTemplate.query(anyString(), any(org.springframework.jdbc.core.ResultSetExtractor.class), any(), any(), any()))
                .thenReturn(Map.of("Delay", 1L));
        assertThat(repository.countFailedByWarehouseAndMonth(1L, 2025, 8)).isEqualTo(1);
        assertThat(repository.failureReasonCountsByWarehouseAndMonth(1L, 2025, 8)).containsKey("Delay");
        assertThat(repository.findFailedByWarehouseAndMonth(1L, 2025, 8, 10)).isNotNull();
        assertThat(repository.festivalFailureAnalysis(FROM, TO)).isNotNull();
        assertThat(repository.totalOrdersByClient(42L)).isEqualTo(1L);
    }

    @Test
    void hasSampleData_falseWhenEmpty() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(0L);
        assertThat(repository.hasSampleData()).isFalse();
    }

    @Test
    void countAffectedByCityAndDate_returnsZeroWhenNull() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq("Pune"), eq(DATE))).thenReturn(null);
        assertThat(repository.countAffectedByCityAndDate("Pune", DATE)).isZero();
    }

    @Test
    void findClientName_returnsNullWhenMissing() throws Exception {
        when(jdbcTemplate.query(anyString(), any(org.springframework.jdbc.core.ResultSetExtractor.class), eq(99L)))
                .thenAnswer(invocation -> {
                    ResultSet rs = mock(ResultSet.class);
                    when(rs.next()).thenReturn(false);
                    @SuppressWarnings("unchecked")
                    org.springframework.jdbc.core.ResultSetExtractor<String> extractor = invocation.getArgument(1);
                    return extractor.extractData(rs);
                });

        assertThat(repository.findClientName(99L)).isNull();
    }

    @Test
    void findWarehouse_returnsNullWhenMissing() throws Exception {
        when(jdbcTemplate.query(anyString(), any(org.springframework.jdbc.core.ResultSetExtractor.class), eq(99L)))
                .thenAnswer(invocation -> {
                    ResultSet rs = mock(ResultSet.class);
                    when(rs.next()).thenReturn(false);
                    @SuppressWarnings("unchecked")
                    org.springframework.jdbc.core.ResultSetExtractor<AnalyticsOrderRepository.WarehouseInfo> extractor =
                            invocation.getArgument(1);
                    return extractor.extractData(rs);
                });

        assertThat(repository.findWarehouse(99L)).isNull();
    }

    @Test
    void findFailedByClient_mapsEnrichedOrderRow() throws Exception {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(42L), any(), any(), eq(10)))
                .thenAnswer(invocation -> List.of(invokeEnrichedOrderMapper(invocation.getArgument(1))));

        assertThat(repository.findFailedByClient(42L, FROM, TO, 10))
                .singleElement()
                .extracting(EnrichedOrder::city)
                .isEqualTo("Pune");
    }

    @Test
    void findFestivalOrders_mapsEnrichedOrderRow() throws Exception {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(), any(), eq(5)))
                .thenAnswer(invocation -> List.of(invokeEnrichedOrderMapper(invocation.getArgument(1))));

        assertThat(repository.findFestivalOrders(FROM, TO, 5)).hasSize(1);
    }

    @Test
    void failureReasonCountsByCityAndMonth_delegatesToRangeQuery() {
        when(jdbcTemplate.query(anyString(), any(org.springframework.jdbc.core.ResultSetExtractor.class),
                eq("Pune"), any(), any()))
                .thenReturn(Map.of("Traffic", 2L));

        assertThat(repository.failureReasonCountsByCityAndMonth("Pune", YearMonth.of(2025, 3)))
                .containsEntry("Traffic", 2L);
    }

    @Test
    void warehouseNoteCountsByWarehouseAndMonth_returnsNotes() throws Exception {
        when(jdbcTemplate.query(anyString(), any(org.springframework.jdbc.core.ResultSetExtractor.class),
                eq(1L), eq(2025), eq(3)))
                .thenAnswer(invocation -> {
                    ResultSet rs = mock(ResultSet.class);
                    when(rs.next()).thenReturn(true, false);
                    when(rs.getString("note")).thenReturn("Slow packing");
                    when(rs.getLong("cnt")).thenReturn(4L);
                    @SuppressWarnings("unchecked")
                    org.springframework.jdbc.core.ResultSetExtractor<Map<String, Long>> extractor =
                            invocation.getArgument(1);
                    return extractor.extractData(rs);
                });

        assertThat(repository.warehouseNoteCountsByWarehouseAndMonth(1L, 2025, 3))
                .containsEntry("Slow packing", 4L);
    }

    @Test
    void failureRateByClient_returnsRate() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Double.class), eq(42L))).thenReturn(0.27);
        assertThat(repository.failureRateByClient(42L)).isEqualTo(0.27);
    }

    @Test
    void avgOrdersPerWarehouseForClient_returnsRows() {
        var row = new AnalyticsOrderRepository.WarehouseLoadRow(1L, "WH", "Pune", 1000, 50L);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(42L))).thenReturn(List.of(row));

        assertThat(repository.avgOrdersPerWarehouseForClient(42L)).containsExactly(row);
    }

    @Test
    void findFailedByWarehouseAndMonth_mapsEnrichedOrderRow() throws Exception {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(1L), eq(2025), eq(3), eq(5)))
                .thenAnswer(invocation -> List.of(invokeEnrichedOrderMapper(invocation.getArgument(1))));

        assertThat(repository.findFailedByWarehouseAndMonth(1L, 2025, 3, 5)).hasSize(1);
    }

    @Test
    void countFailedByClient_returnsZeroWhenNull() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(42L), any(), any())).thenReturn(null);
        assertThat(repository.countFailedByClient(42L, FROM, TO)).isZero();
    }

    @Test
    void failureRateByClient_returnsZeroWhenNull() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Double.class), eq(42L))).thenReturn(null);
        assertThat(repository.failureRateByClient(42L)).isZero();
    }

    @Test
    void festivalFailureAnalysis_mapsRows() {
        var row = new AnalyticsOrderRepository.FestivalRow("Festival", "Traffic", 3L, 900.0);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(), any()))
                .thenReturn(List.of(row));

        assertThat(repository.festivalFailureAnalysis(FROM, TO)).containsExactly(row);
    }

    @Test
    void findAffectedByCityAndDate_mapsAllEnrichedOrderFields() throws Exception {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq("Pune"), eq(DATE), eq(1)))
                .thenAnswer(invocation -> {
                    ResultSet rs = mock(ResultSet.class);
                    when(rs.getLong("order_id")).thenReturn(1L);
                    when(rs.getString("city")).thenReturn("Pune");
                    when(rs.getString("status")).thenReturn("Failed");
                    when(rs.getString("failure_reason")).thenReturn("Traffic");
                    when(rs.getBoolean("is_delayed")).thenReturn(true);
                    when(rs.getBoolean("is_failed")).thenReturn(true);
                    when(rs.getObject("client_id")).thenReturn(42L);
                    when(rs.getLong("client_id")).thenReturn(42L);
                    when(rs.getString("client_name")).thenReturn("Acme");
                    when(rs.getObject("warehouse_id")).thenReturn(7L);
                    when(rs.getLong("warehouse_id")).thenReturn(7L);
                    when(rs.getString("warehouse_name")).thenReturn("WH");
                    when(rs.getString("warehouse_notes")).thenReturn("Slow");
                    when(rs.getString("gps_delay_notes")).thenReturn("GPS");
                    when(rs.getObject("driver_id")).thenReturn(9L);
                    when(rs.getLong("driver_id")).thenReturn(9L);
                    when(rs.getString("traffic_condition")).thenReturn("Heavy");
                    when(rs.getString("weather_condition")).thenReturn("Rain");
                    when(rs.getString("event_type")).thenReturn("Festival");
                    when(rs.getString("feedback_text")).thenReturn("Late");
                    when(rs.getString("feedback_sentiment")).thenReturn("Negative");
                    when(rs.getTimestamp("order_date"))
                            .thenReturn(java.sql.Timestamp.from(Instant.parse("2025-03-17T10:00:00Z")));
                    RowMapper<EnrichedOrder> mapper = invocation.getArgument(1);
                    return List.of(mapper.mapRow(rs, 0));
                });

        EnrichedOrder order = repository.findAffectedByCityAndDate("Pune", DATE, 1).get(0);
        assertThat(order.clientId()).isEqualTo(42L);
        assertThat(order.warehouseName()).isEqualTo("WH");
        assertThat(order.orderDate()).isNotNull();
    }

    private static EnrichedOrder invokeEnrichedOrderMapper(RowMapper<EnrichedOrder> mapper) throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("order_id")).thenReturn(100L);
        when(rs.getString("city")).thenReturn("Pune");
        when(rs.getString("status")).thenReturn("Failed");
        when(rs.getString("failure_reason")).thenReturn("Traffic");
        when(rs.getBoolean("is_delayed")).thenReturn(true);
        when(rs.getBoolean("is_failed")).thenReturn(true);
        when(rs.getObject("client_id")).thenReturn(null);
        when(rs.getString("client_name")).thenReturn(null);
        when(rs.getObject("warehouse_id")).thenReturn(null);
        when(rs.getString("warehouse_name")).thenReturn(null);
        when(rs.getString("warehouse_notes")).thenReturn(null);
        when(rs.getString("gps_delay_notes")).thenReturn(null);
        when(rs.getObject("driver_id")).thenReturn(null);
        when(rs.getString("traffic_condition")).thenReturn("Heavy");
        when(rs.getString("weather_condition")).thenReturn("Rain");
        when(rs.getString("event_type")).thenReturn("Festival");
        when(rs.getString("feedback_text")).thenReturn("Late");
        when(rs.getString("feedback_sentiment")).thenReturn("Negative");
        when(rs.getTimestamp("order_date")).thenReturn(null);
        return mapper.mapRow(rs, 0);
    }

    private static EnrichedOrder sampleOrder() {
        return new EnrichedOrder(
                1L, "Pune", "Failed", "Warehouse delay", true, true,
                42L, "Acme", 7L, "WH-B", "Slow packing", "GPS lag", 9L,
                "Heavy", "Rain", "Festival", "Late", "Negative",
                Instant.parse("2025-03-17T10:00:00Z"));
    }
}
