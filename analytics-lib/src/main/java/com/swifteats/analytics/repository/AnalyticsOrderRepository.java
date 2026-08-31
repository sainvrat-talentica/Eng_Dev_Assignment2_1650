package com.swifteats.analytics.repository;

import com.swifteats.analytics.model.EnrichedOrder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class AnalyticsOrderRepository {

    private static final String ENRICHED_ORDER_SELECT = """
            SELECT o.order_id, o.city, o.status, o.failure_reason,
                   o.is_delayed, o.is_failed, o.client_id, c.client_name,
                   w.warehouse_id, w.warehouse_name, wl.notes AS warehouse_notes,
                   fl.gps_delay_notes, fl.driver_id,
                   ef.traffic_condition, ef.weather_condition, ef.event_type,
                   fb.feedback_text, fb.sentiment AS feedback_sentiment,
                   o.order_date
            FROM analytics."order" o
            LEFT JOIN analytics.client c ON c.client_id = o.client_id
            LEFT JOIN analytics.warehouse_log wl ON wl.order_id = o.order_id
            LEFT JOIN analytics.warehouse w ON w.warehouse_id = wl.warehouse_id
            LEFT JOIN analytics.fleet_log fl ON fl.order_id = o.order_id
            LEFT JOIN analytics.external_factor ef ON ef.order_id = o.order_id
            LEFT JOIN analytics.feedback fb ON fb.order_id = o.order_id
            """;

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<EnrichedOrder> enrichedOrderMapper = this::mapEnrichedOrder;

    public AnalyticsOrderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean hasSampleData() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM analytics.\"order\"",
                Long.class);
        return count != null && count > 0;
    }

    public long countAffectedByCityAndDate(String city, LocalDate date) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM analytics."order" o
                WHERE o.city = ?
                  AND DATE(o.order_date) = ?
                  AND (o.is_delayed = true OR o.status = 'Failed')
                """, Long.class, city, date);
        return count == null ? 0 : count;
    }

    public Map<String, Long> failureReasonCountsByCityAndDate(String city, LocalDate date) {
        return jdbcTemplate.query("""
                SELECT COALESCE(o.failure_reason, 'Unknown') AS reason, COUNT(*) AS cnt
                FROM analytics."order" o
                WHERE o.city = ?
                  AND DATE(o.order_date) = ?
                  AND (o.is_delayed = true OR o.status = 'Failed')
                GROUP BY COALESCE(o.failure_reason, 'Unknown')
                ORDER BY cnt DESC
                """, rs -> {
            Map<String, Long> counts = new LinkedHashMap<>();
            while (rs.next()) {
                counts.put(rs.getString("reason"), rs.getLong("cnt"));
            }
            return counts;
        }, city, date);
    }

    public long countHeavyTrafficByCityAndDate(String city, LocalDate date) {
        return countWithJoin("""
                SELECT COUNT(DISTINCT o.order_id)
                FROM analytics."order" o
                JOIN analytics.external_factor ef ON ef.order_id = o.order_id
                WHERE o.city = ?
                  AND DATE(o.order_date) = ?
                  AND (o.is_delayed = true OR o.status = 'Failed')
                  AND ef.traffic_condition = 'Heavy'
                """, city, date);
    }

    public long countSlowPackingByCityAndDate(String city, LocalDate date) {
        return countWithJoin("""
                SELECT COUNT(DISTINCT o.order_id)
                FROM analytics."order" o
                JOIN analytics.warehouse_log wl ON wl.order_id = o.order_id
                WHERE o.city = ?
                  AND DATE(o.order_date) = ?
                  AND (o.is_delayed = true OR o.status = 'Failed')
                  AND wl.notes LIKE '%Slow packing%'
                """, city, date);
    }

    public long countNegativeFeedbackByCityAndDate(String city, LocalDate date) {
        return countWithJoin("""
                SELECT COUNT(DISTINCT o.order_id)
                FROM analytics."order" o
                JOIN analytics.feedback fb ON fb.order_id = o.order_id
                WHERE o.city = ?
                  AND DATE(o.order_date) = ?
                  AND (o.is_delayed = true OR o.status = 'Failed')
                  AND fb.sentiment = 'Negative'
                """, city, date);
    }

    public List<EnrichedOrder> findAffectedByCityAndDate(String city, LocalDate date, int limit) {
        return jdbcTemplate.query(
                ENRICHED_ORDER_SELECT + """
                        WHERE o.city = ?
                          AND DATE(o.order_date) = ?
                          AND (o.is_delayed = true OR o.status = 'Failed')
                        LIMIT ?
                        """,
                enrichedOrderMapper,
                city, date, limit);
    }

    public List<FailureReasonRow> failureBreakdownByClient(long clientId, Instant from, Instant to) {
        return jdbcTemplate.query("""
                SELECT o.failure_reason,
                       COUNT(*) AS cnt,
                       STRING_AGG(DISTINCT fl.gps_delay_notes, ', ') AS fleet_issues,
                       STRING_AGG(DISTINCT wl.notes, ', ') AS warehouse_issues
                FROM analytics."order" o
                LEFT JOIN analytics.fleet_log fl ON fl.order_id = o.order_id
                LEFT JOIN analytics.warehouse_log wl ON wl.order_id = o.order_id
                WHERE o.client_id = ?
                  AND o.status = 'Failed'
                  AND o.order_date >= ? AND o.order_date < ?
                GROUP BY o.failure_reason
                ORDER BY cnt DESC
                """, (rs, rowNum) -> new FailureReasonRow(
                rs.getString("failure_reason"),
                rs.getLong("cnt"),
                rs.getString("fleet_issues"),
                rs.getString("warehouse_issues")
        ), clientId, Timestamp.from(from), Timestamp.from(to));
    }

    public int countFailedByClient(long clientId, Instant from, Instant to) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM analytics."order" o
                WHERE o.client_id = ?
                  AND o.status = 'Failed'
                  AND o.order_date >= ? AND o.order_date < ?
                """, Long.class, clientId, Timestamp.from(from), Timestamp.from(to));
        return count == null ? 0 : count.intValue();
    }

    public List<EnrichedOrder> findFailedByClient(long clientId, Instant from, Instant to, int limit) {
        return jdbcTemplate.query(
                ENRICHED_ORDER_SELECT + """
                        WHERE o.client_id = ?
                          AND o.status = 'Failed'
                          AND o.order_date >= ? AND o.order_date < ?
                        LIMIT ?
                        """,
                enrichedOrderMapper,
                clientId, Timestamp.from(from), Timestamp.from(to), limit);
    }

    public String findClientName(long clientId) {
        return jdbcTemplate.query("""
                SELECT client_name FROM analytics.client WHERE client_id = ?
                """, rs -> rs.next() ? rs.getString("client_name") : null, clientId);
    }

    public WarehouseInfo findWarehouse(long warehouseId) {
        return jdbcTemplate.query("""
                SELECT warehouse_id, warehouse_name, city, capacity
                FROM analytics.warehouse WHERE warehouse_id = ?
                """, rs -> rs.next()
                ? new WarehouseInfo(
                rs.getLong("warehouse_id"),
                rs.getString("warehouse_name"),
                rs.getString("city"),
                rs.getInt("capacity"))
                : null, warehouseId);
    }

    public int countFailedByWarehouseAndMonth(long warehouseId, int year, int month) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM analytics."order" o
                JOIN analytics.warehouse_log wl ON wl.order_id = o.order_id
                WHERE wl.warehouse_id = ?
                  AND o.status = 'Failed'
                  AND EXTRACT(YEAR FROM o.order_date) = ?
                  AND EXTRACT(MONTH FROM o.order_date) = ?
                """, Long.class, warehouseId, year, month);
        return count == null ? 0 : count.intValue();
    }

    public Map<String, Long> failureReasonCountsByWarehouseAndMonth(long warehouseId, int year, int month) {
        return jdbcTemplate.query("""
                SELECT o.failure_reason, COUNT(*) AS cnt
                FROM analytics."order" o
                JOIN analytics.warehouse_log wl ON wl.order_id = o.order_id
                WHERE wl.warehouse_id = ?
                  AND o.status = 'Failed'
                  AND EXTRACT(YEAR FROM o.order_date) = ?
                  AND EXTRACT(MONTH FROM o.order_date) = ?
                GROUP BY o.failure_reason
                ORDER BY cnt DESC
                """, rs -> {
            Map<String, Long> counts = new LinkedHashMap<>();
            while (rs.next()) {
                counts.put(rs.getString("failure_reason"), rs.getLong("cnt"));
            }
            return counts;
        }, warehouseId, year, month);
    }

    public Map<String, Long> warehouseNoteCountsByWarehouseAndMonth(long warehouseId, int year, int month) {
        return jdbcTemplate.query("""
                SELECT COALESCE(NULLIF(wl.notes, ''), 'None') AS note, COUNT(*) AS cnt
                FROM analytics."order" o
                JOIN analytics.warehouse_log wl ON wl.order_id = o.order_id
                WHERE wl.warehouse_id = ?
                  AND o.status = 'Failed'
                  AND EXTRACT(YEAR FROM o.order_date) = ?
                  AND EXTRACT(MONTH FROM o.order_date) = ?
                GROUP BY COALESCE(NULLIF(wl.notes, ''), 'None')
                ORDER BY cnt DESC
                """, rs -> {
            Map<String, Long> counts = new LinkedHashMap<>();
            while (rs.next()) {
                counts.put(rs.getString("note"), rs.getLong("cnt"));
            }
            return counts;
        }, warehouseId, year, month);
    }

    public List<EnrichedOrder> findFailedByWarehouseAndMonth(long warehouseId, int year, int month, int limit) {
        return jdbcTemplate.query(
                ENRICHED_ORDER_SELECT + """
                        WHERE wl.warehouse_id = ?
                          AND o.status = 'Failed'
                          AND EXTRACT(YEAR FROM o.order_date) = ?
                          AND EXTRACT(MONTH FROM o.order_date) = ?
                        LIMIT ?
                        """,
                enrichedOrderMapper,
                warehouseId, year, month, limit);
    }

    public Map<String, Long> failureReasonCountsByCityAndMonth(String city, YearMonth month) {
        return failureReasonCountsForCityInRange(city, month.atDay(1), month.atEndOfMonth());
    }

    private Map<String, Long> failureReasonCountsForCityInRange(String city, LocalDate start, LocalDate end) {
        return jdbcTemplate.query("""
                SELECT o.failure_reason, COUNT(*) AS cnt
                FROM analytics."order" o
                WHERE o.status = 'Failed'
                  AND o.city = ?
                  AND DATE(o.order_date) >= ? AND DATE(o.order_date) <= ?
                GROUP BY o.failure_reason
                ORDER BY cnt DESC
                """, rs -> {
            Map<String, Long> counts = new LinkedHashMap<>();
            while (rs.next()) {
                counts.put(rs.getString("failure_reason"), rs.getLong("cnt"));
            }
            return counts;
        }, city, start, end);
    }

    public List<FestivalRow> festivalFailureAnalysis(Instant from, Instant to) {
        return jdbcTemplate.query("""
                SELECT ef.event_type, o.failure_reason, COUNT(*) AS cnt,
                       AVG(w.capacity) AS avg_capacity
                FROM analytics."order" o
                JOIN analytics.external_factor ef ON ef.order_id = o.order_id
                LEFT JOIN analytics.warehouse_log wl ON wl.order_id = o.order_id
                LEFT JOIN analytics.warehouse w ON w.warehouse_id = wl.warehouse_id
                WHERE ef.event_type IN ('Festival', 'Holiday')
                  AND o.order_date >= ? AND o.order_date < ?
                GROUP BY ef.event_type, o.failure_reason
                ORDER BY ef.event_type, cnt DESC
                """, (rs, rowNum) -> new FestivalRow(
                rs.getString("event_type"),
                rs.getString("failure_reason"),
                rs.getLong("cnt"),
                rs.getDouble("avg_capacity")
        ), Timestamp.from(from), Timestamp.from(to));
    }

    public List<EnrichedOrder> findFestivalOrders(Instant from, Instant to, int limit) {
        return jdbcTemplate.query(
                ENRICHED_ORDER_SELECT + """
                        WHERE ef.event_type IN ('Festival', 'Holiday')
                          AND o.order_date >= ? AND o.order_date < ?
                        LIMIT ?
                        """,
                enrichedOrderMapper,
                Timestamp.from(from), Timestamp.from(to), limit);
    }

    public double failureRateByClient(long clientId) {
        Double rate = jdbcTemplate.queryForObject("""
                SELECT CASE WHEN COUNT(*) = 0 THEN 0
                       ELSE COUNT(*) FILTER (WHERE status = 'Failed')::float / COUNT(*)
                       END
                FROM analytics."order"
                WHERE client_id = ?
                """, Double.class, clientId);
        return rate == null ? 0.0 : rate;
    }

    public List<WarehouseLoadRow> avgOrdersPerWarehouseForClient(long clientId) {
        return jdbcTemplate.query("""
                SELECT w.warehouse_id, w.warehouse_name, w.city, w.capacity,
                       COUNT(DISTINCT o.order_id) AS order_count
                FROM analytics."order" o
                JOIN analytics.warehouse_log wl ON wl.order_id = o.order_id
                JOIN analytics.warehouse w ON w.warehouse_id = wl.warehouse_id
                WHERE o.client_id = ?
                GROUP BY w.warehouse_id, w.warehouse_name, w.city, w.capacity
                ORDER BY order_count DESC
                """, (rs, rowNum) -> new WarehouseLoadRow(
                rs.getLong("warehouse_id"),
                rs.getString("warehouse_name"),
                rs.getString("city"),
                rs.getInt("capacity"),
                rs.getLong("order_count")
        ), clientId);
    }

    public long totalOrdersByClient(long clientId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM analytics.\"order\" WHERE client_id = ?",
                Long.class, clientId);
        return count == null ? 0 : count;
    }

    private long countWithJoin(String sql, String city, LocalDate date) {
        Long count = jdbcTemplate.queryForObject(sql, Long.class, city, date);
        return count == null ? 0 : count;
    }

    private EnrichedOrder mapEnrichedOrder(ResultSet rs, int rowNum) throws SQLException {
        Timestamp orderDate = rs.getTimestamp("order_date");
        return new EnrichedOrder(
                rs.getLong("order_id"),
                rs.getString("city"),
                rs.getString("status"),
                rs.getString("failure_reason"),
                rs.getBoolean("is_delayed"),
                rs.getBoolean("is_failed"),
                rs.getObject("client_id") != null ? rs.getLong("client_id") : null,
                rs.getString("client_name"),
                rs.getObject("warehouse_id") != null ? rs.getLong("warehouse_id") : null,
                rs.getString("warehouse_name"),
                rs.getString("warehouse_notes"),
                rs.getString("gps_delay_notes"),
                rs.getObject("driver_id") != null ? rs.getLong("driver_id") : null,
                rs.getString("traffic_condition"),
                rs.getString("weather_condition"),
                rs.getString("event_type"),
                rs.getString("feedback_text"),
                rs.getString("feedback_sentiment"),
                orderDate != null ? orderDate.toInstant() : null
        );
    }

    public record FailureReasonRow(
            String failureReason,
            long count,
            String fleetIssues,
            String warehouseIssues
    ) {
    }

    public record WarehouseInfo(long warehouseId, String warehouseName, String city, int capacity) {
    }

    public record FestivalRow(String eventType, String failureReason, long count, double avgCapacity) {
    }

    public record WarehouseLoadRow(
            long warehouseId,
            String warehouseName,
            String city,
            int capacity,
            long orderCount
    ) {
    }
}
