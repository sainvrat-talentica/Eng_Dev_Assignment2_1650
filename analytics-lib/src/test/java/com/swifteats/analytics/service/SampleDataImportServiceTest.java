package com.swifteats.analytics.service;

import com.swifteats.analytics.config.AnalyticsProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class SampleDataImportServiceTest {

    @TempDir
    Path tempDir;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private TransactionTemplate transactionTemplate;

    private SampleDataImportService service;

    @BeforeEach
    void setUp() {
        service = new SampleDataImportService(
                jdbcTemplate,
                new AnalyticsProperties(false, false, "sample-data", 500),
                transactionTemplate,
                new ImportLock());
    }

    @Test
    void resolveDatasetPath_returnsAbsoluteDirectory() throws Exception {
        Path dataset = Files.createDirectory(tempDir.resolve("dataset"));

        Path resolved = service.resolveDatasetPath(dataset.toString());

        assertThat(resolved).isEqualTo(dataset.toAbsolutePath().normalize());
    }

    @Test
    void resolveDatasetPath_throwsWhenMissing() {
        assertThatThrownBy(() -> service.resolveDatasetPath(tempDir.resolve("missing").toString()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Dataset directory not found");
    }

    @Test
    void importAll_rejectsMissingCsv() throws Exception {
        Path dataset = Files.createDirectory(tempDir.resolve("partial"));
        Files.writeString(dataset.resolve("clients.csv"), "h gfxeader\n");

        assertThatThrownBy(() -> service.importAll(dataset))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing required CSV");
    }
}
