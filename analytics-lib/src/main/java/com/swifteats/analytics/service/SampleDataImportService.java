package com.swifteats.analytics.service;

import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.swifteats.analytics.config.AnalyticsProperties;
import com.swifteats.analytics.dto.ImportResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.swifteats.analytics.util.CsvParsingUtils.blankToNull;
import static com.swifteats.analytics.util.CsvParsingUtils.isDelayed;
import static com.swifteats.analytics.util.CsvParsingUtils.isFailedStatus;
import static com.swifteats.analytics.util.CsvParsingUtils.parseDateTime;
import static com.swifteats.analytics.util.CsvParsingUtils.parseDecimal;
import static com.swifteats.analytics.util.CsvParsingUtils.parseInteger;
import static com.swifteats.analytics.util.CsvParsingUtils.parseLong;
import static com.swifteats.analytics.util.CsvParsingUtils.toInstant;

@Service
@ServiceScope(ServiceName.ANALYTICS)
public class SampleDataImportService {

    private static final Logger log = LoggerFactory.getLogger(SampleDataImportService.class);

    private static final List<String> IMPORT_FILES = List.of(
            "clients.csv",
            "warehouses.csv",
            "drivers.csv",
            "orders.csv",
            "warehouse_logs.csv",
            "fleet_logs.csv",
            "feedback.csv",
            "external_factors.csv"
    );

    private final JdbcTemplate jdbcTemplate;
    private final AnalyticsProperties properties;
    private final TransactionTemplate transactionTemplate;
    private final ImportLock importLock;

    public SampleDataImportService(
            JdbcTemplate jdbcTemplate,
            AnalyticsProperties properties,
            TransactionTemplate transactionTemplate,
            ImportLock importLock) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
        this.transactionTemplate = transactionTemplate;
        this.importLock = importLock;
    }

    public Path resolveDatasetPath(String configuredPath) {
        Path path = Path.of(configuredPath);
        if (path.isAbsolute()) {
            if (Files.isDirectory(path)) {
                return path.normalize();
            }
            throw new IllegalArgumentException(
                    "Dataset directory not found for path '" + configuredPath + "'.");
        }

        if (Files.isDirectory(path)) {
            return path.toAbsolutePath().normalize();
        }

        Path cwd = Path.of(System.getProperty("user.dir"));
        Path[] candidates = {
                cwd.resolve(configuredPath),
                cwd.resolve("assignment-2").resolve("sample-data"),
                cwd.getParent() != null ? cwd.getParent().resolve("sample-data") : null,
                cwd.getParent() != null ? cwd.getParent().resolve(configuredPath) : null,
                cwd.getParent() != null && cwd.getParent().getParent() != null
                        ? cwd.getParent().getParent().resolve(configuredPath)
                        : null,
                cwd.resolve("realTime-foodDelivery-service").resolve(configuredPath)
        };

        for (Path candidate : candidates) {
            if (candidate != null && Files.isDirectory(candidate)) {
                return candidate.toAbsolutePath().normalize();
            }
        }

        throw new IllegalArgumentException(
                "Dataset directory not found for path '" + configuredPath
                        + "'. Set SWIFTEATS_DATASET_PATH or run from repo root.");
    }

    public ImportResult importAll(Path datasetDir) {
        return importLock.execute(() -> doImport(datasetDir));
    }

    public ImportResult importFromConfiguredPath() {
        Path datasetDir = resolveDatasetPath(properties.datasetPath());
        return importAll(datasetDir);
    }

    private ImportResult doImport(Path datasetDir) {
        long start = System.currentTimeMillis();
        validateDataset(datasetDir);

        log.info("Truncating analytics tables before import");
        runInTransaction(this::truncateAnalyticsTables);

        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("client", runInTransaction(() -> importClients(datasetDir)));
        counts.put("warehouse", runInTransaction(() -> importWarehouses(datasetDir)));
        counts.put("driver", runInTransaction(() -> importDrivers(datasetDir)));
        counts.put("order", runInTransaction(() -> importOrders(datasetDir)));
        counts.put("warehouse_log", runInTransaction(() -> importWarehouseLogs(datasetDir)));
        counts.put("fleet_log", runInTransaction(() -> importFleetLogs(datasetDir)));
        counts.put("feedback", runInTransaction(() -> importFeedback(datasetDir)));
        counts.put("external_factor", runInTransaction(() -> importExternalFactors(datasetDir)));

        long duration = System.currentTimeMillis() - start;
        log.info("Import complete in {} ms: {}", duration, counts);
        return ImportResult.success(datasetDir.toString(), counts, duration);
    }

    private void runInTransaction(Runnable action) {
        transactionTemplate.executeWithoutResult(status -> action.run());
    }

    private long runInTransaction(java.util.function.Supplier<Long> action) {
        Long count = transactionTemplate.execute(status -> action.get());
        return count != null ? count : 0L;
    }

    private void validateDataset(Path datasetDir) {
        if (!Files.isDirectory(datasetDir)) {
            throw new IllegalArgumentException("Not a directory: " + datasetDir);
        }
        for (String file : IMPORT_FILES) {
            Path csv = datasetDir.resolve(file);
            if (!Files.isRegularFile(csv)) {
                throw new IllegalArgumentException("Missing required CSV: " + csv);
            }
        }
    }

    private void truncateAnalyticsTables() {
        jdbcTemplate.execute("""
                TRUNCATE TABLE
                    analytics.delivery_insight,
                    analytics.external_factor,
                    analytics.feedback,
                    analytics.fleet_log,
                    analytics.warehouse_log,
                    analytics."order",
                    analytics.driver,
                    analytics.warehouse,
                    analytics.client
                RESTART IDENTITY CASCADE
                """);
    }

    private long importClients(Path datasetDir) {
        String sql = """
                INSERT INTO analytics.client (
                    client_id, client_name, gst_number, contact_person, contact_phone,
                    contact_email, address_line1, address_line2, city, state, pincode, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        return batchInsert(sql, datasetDir.resolve("clients.csv"), (ps, row) -> {
            ps.setLong(1, parseLong(row[0]));
            ps.setString(2, row[1]);
            ps.setString(3, blankToNull(row[2]));
            ps.setString(4, blankToNull(row[3]));
            ps.setString(5, blankToNull(row[4]));
            ps.setString(6, blankToNull(row[5]));
            ps.setString(7, blankToNull(row[6]));
            ps.setString(8, blankToNull(row[7]));
            ps.setString(9, blankToNull(row[8]));
            ps.setString(10, blankToNull(row[9]));
            ps.setString(11, blankToNull(row[10]));
            setTimestamp(ps, 12, parseDateTime(row[11]));
        });
    }

    private long importWarehouses(Path datasetDir) {
        String sql = """
                INSERT INTO analytics.warehouse (
                    warehouse_id, warehouse_name, state, city, pincode, capacity,
                    manager_name, contact_phone, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        return batchInsert(sql, datasetDir.resolve("warehouses.csv"), (ps, row) -> {
            ps.setLong(1, parseLong(row[0]));
            ps.setString(2, row[1]);
            ps.setString(3, blankToNull(row[2]));
            ps.setString(4, blankToNull(row[3]));
            ps.setString(5, blankToNull(row[4]));
            if (row[5] != null && !row[5].isBlank()) {
                ps.setInt(6, parseInteger(row[5]));
            } else {
                ps.setObject(6, null);
            }
            ps.setString(7, blankToNull(row[6]));
            ps.setString(8, blankToNull(row[7]));
            setTimestamp(ps, 9, parseDateTime(row[8]));
        });
    }

    private long importDrivers(Path datasetDir) {
        String sql = """
                INSERT INTO analytics.driver (
                    driver_id, driver_name, phone, license_number, partner_company,
                    city, state, status, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        return batchInsert(sql, datasetDir.resolve("drivers.csv"), (ps, row) -> {
            ps.setLong(1, parseLong(row[0]));
            ps.setString(2, blankToNull(row[1]));
            ps.setString(3, blankToNull(row[2]));
            ps.setString(4, blankToNull(row[3]));
            ps.setString(5, blankToNull(row[4]));
            ps.setString(6, blankToNull(row[5]));
            ps.setString(7, blankToNull(row[6]));
            ps.setString(8, blankToNull(row[7]));
            setTimestamp(ps, 9, parseDateTime(row[8]));
        });
    }

    private long importOrders(Path datasetDir) {
        String sql = """
                INSERT INTO analytics."order" (
                    order_id, client_id, customer_name, customer_phone,
                    delivery_address_line1, delivery_address_line2, city, state, pincode,
                    order_date, promised_delivery_date, actual_delivery_date,
                    status, payment_mode, amount, failure_reason, created_at,
                    is_delayed, is_failed
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        return batchInsert(sql, datasetDir.resolve("orders.csv"), (ps, row) -> {
            LocalDateTime promised = parseDateTime(row[10]);
            LocalDateTime actual = parseDateTime(row[11]);
            String status = row[12];

            ps.setLong(1, parseLong(row[0]));
            ps.setLong(2, parseLong(row[1]));
            ps.setString(3, blankToNull(row[2]));
            ps.setString(4, blankToNull(row[3]));
            ps.setString(5, blankToNull(row[4]));
            ps.setString(6, blankToNull(row[5]));
            ps.setString(7, row[6]);
            ps.setString(8, blankToNull(row[7]));
            ps.setString(9, blankToNull(row[8]));
            setTimestamp(ps, 10, parseDateTime(row[9]));
            setTimestamp(ps, 11, promised);
            setTimestamp(ps, 12, actual);
            ps.setString(13, status);
            ps.setString(14, blankToNull(row[13]));
            ps.setBigDecimal(15, parseDecimal(row[14]));
            ps.setString(16, blankToNull(row[15]));
            setTimestamp(ps, 17, parseDateTime(row[16]));
            ps.setBoolean(18, isDelayed(actual, promised));
            ps.setBoolean(19, isFailedStatus(status));
        });
    }

    private long importWarehouseLogs(Path datasetDir) {
        String sql = """
                INSERT INTO analytics.warehouse_log (
                    log_id, order_id, warehouse_id, picking_start, picking_end,
                    dispatch_time, notes
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        return batchInsert(sql, datasetDir.resolve("warehouse_logs.csv"), (ps, row) -> {
            ps.setLong(1, parseLong(row[0]));
            ps.setLong(2, parseLong(row[1]));
            ps.setLong(3, parseLong(row[2]));
            setTimestamp(ps, 4, parseDateTime(row[3]));
            setTimestamp(ps, 5, parseDateTime(row[4]));
            setTimestamp(ps, 6, parseDateTime(row[5]));
            ps.setString(7, blankToNull(row[6]));
        });
    }

    private long importFleetLogs(Path datasetDir) {
        String sql = """
                INSERT INTO analytics.fleet_log (
                    fleet_log_id, order_id, driver_id, vehicle_number, route_code,
                    gps_delay_notes, departure_time, arrival_time, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        return batchInsert(sql, datasetDir.resolve("fleet_logs.csv"), (ps, row) -> {
            ps.setLong(1, parseLong(row[0]));
            ps.setLong(2, parseLong(row[1]));
            Long driverId = parseLong(row[2]);
            if (driverId != null) {
                ps.setLong(3, driverId);
            } else {
                ps.setObject(3, null);
            }
            ps.setString(4, blankToNull(row[3]));
            ps.setString(5, blankToNull(row[4]));
            ps.setString(6, blankToNull(row[5]));
            setTimestamp(ps, 7, parseDateTime(row[6]));
            setTimestamp(ps, 8, parseDateTime(row[7]));
            setTimestamp(ps, 9, parseDateTime(row[8]));
        });
    }

    private long importFeedback(Path datasetDir) {
        String sql = """
                INSERT INTO analytics.feedback (
                    feedback_id, order_id, customer_name, feedback_text,
                    sentiment, rating, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        return batchInsert(sql, datasetDir.resolve("feedback.csv"), (ps, row) -> {
            ps.setLong(1, parseLong(row[0]));
            ps.setLong(2, parseLong(row[1]));
            ps.setString(3, blankToNull(row[2]));
            ps.setString(4, blankToNull(row[3]));
            ps.setString(5, blankToNull(row[4]));
            Integer rating = parseInteger(row[5]);
            if (rating != null) {
                ps.setInt(6, rating);
            } else {
                ps.setObject(6, null);
            }
            setTimestamp(ps, 7, parseDateTime(row[6]));
        });
    }

    private long importExternalFactors(Path datasetDir) {
        String sql = """
                INSERT INTO analytics.external_factor (
                    factor_id, order_id, traffic_condition, weather_condition,
                    event_type, recorded_at
                ) VALUES (?, ?, ?, ?, ?, ?)
                """;
        return batchInsert(sql, datasetDir.resolve("external_factors.csv"), (ps, row) -> {
            ps.setLong(1, parseLong(row[0]));
            ps.setLong(2, parseLong(row[1]));
            ps.setString(3, blankToNull(row[2]));
            ps.setString(4, blankToNull(row[3]));
            ps.setString(5, blankToNull(row[4]));
            setTimestamp(ps, 6, parseDateTime(row[5]));
        });
    }

    @FunctionalInterface
    private interface RowBinder {
        void bind(java.sql.PreparedStatement ps, String[] row) throws java.sql.SQLException;
    }

    private long batchInsert(String sql, Path csvPath, RowBinder binder) {
        List<String[]> rows = readCsv(csvPath);
        int batchSize = properties.batchSize();
        int total = 0;

        for (int i = 0; i < rows.size(); i += batchSize) {
            List<String[]> chunk = rows.subList(i, Math.min(i + batchSize, rows.size()));
            jdbcTemplate.batchUpdate(sql, chunk, chunk.size(), binder::bind);
            total += chunk.size();
        }

        log.info("Imported {} rows from {}", total, csvPath.getFileName());
        return total;
    }

    private List<String[]> readCsv(Path csvPath) {
        try (Reader reader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8);
             CSVReader csvReader = new CSVReader(reader)) {
            List<String[]> all = csvReader.readAll();
            if (all.isEmpty()) {
                return List.of();
            }
            return new ArrayList<>(all.subList(1, all.size()));
        } catch (IOException | CsvException ex) {
            throw new IllegalStateException("Failed to read CSV: " + csvPath, ex);
        }
    }

    private static void setTimestamp(java.sql.PreparedStatement ps, int index, LocalDateTime value)
            throws java.sql.SQLException {
        if (value == null) {
            ps.setObject(index, null);
        } else {
            ps.setTimestamp(index, Timestamp.from(toInstant(value)));
        }
    }
}
