package com.swifteats.analytics.runner;

import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;

import com.swifteats.analytics.config.AnalyticsProperties;
import com.swifteats.analytics.dto.ImportResult;
import com.swifteats.analytics.service.SampleDataImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "swifteats.analytics.import-on-startup", havingValue = "true")
@ServiceScope(ServiceName.ANALYTICS)
public class SampleDataImportRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SampleDataImportRunner.class);

    private final SampleDataImportService importService;
    private final AnalyticsProperties properties;

    public SampleDataImportRunner(SampleDataImportService importService, AnalyticsProperties properties) {
        this.importService = importService;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (args.containsOption("import-sample-data")) {
            log.info("Import triggered via --import-sample-data flag");
        }
        log.info("Import-on-startup enabled; loading dataset from {}", properties.datasetPath());
        ImportResult result = importService.importFromConfiguredPath();
        log.info("Startup import finished: {}", result.rowCounts());
    }
}
