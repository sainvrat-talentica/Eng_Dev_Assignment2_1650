package com.swifteats.analytics.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = com.swifteats.test.AnalyticsLibTestApplication.class)
@Testcontainers(disabledWithoutDocker = true)
class SampleDataImportServiceIntegrationTest {

    static {
        System.setProperty("swifteats.service.name", "ANALYTICS");
    }

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("swifteats_test")
            .withUsername("swifteats")
            .withPassword("swifteats");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("swifteats.analytics.import-on-startup", () -> "false");
    }

    @Autowired
    private SampleDataImportService importService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void importAll_loadsExpectedRowCounts() {
        Path datasetDir = importService.resolveDatasetPath("sample-data");
        var result = importService.importAll(datasetDir);

        assertThat(result.success()).isTrue();
        assertThat(result.rowCounts().get("client")).isEqualTo(748L);
        assertThat(result.rowCounts().get("warehouse")).isEqualTo(50L);
        assertThat(result.rowCounts().get("driver")).isEqualTo(2000L);
        assertThat(result.rowCounts().get("order")).isEqualTo(14947L);
        assertThat(result.rowCounts().get("warehouse_log")).isEqualTo(10000L);
        assertThat(result.rowCounts().get("fleet_log")).isEqualTo(10000L);
        assertThat(result.rowCounts().get("feedback")).isEqualTo(10000L);
        assertThat(result.rowCounts().get("external_factor")).isEqualTo(10000L);

        Long failedOrders = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM analytics.\"order\" WHERE is_failed = true", Long.class);
        assertThat(failedOrders).isEqualTo(2004L);

        Long delayedOrders = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM analytics.\"order\" WHERE is_delayed = true", Long.class);
        assertThat(delayedOrders).isPositive();
    }
}
