package com.swifteats.analytics.controller;

import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;

import com.swifteats.common.config.OpenApiConfig;
import com.swifteats.analytics.dto.ImportResult;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import com.swifteats.analytics.service.SampleDataImportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/analytics")
@SecurityRequirement(name = OpenApiConfig.ADMIN_API_KEY)
@ServiceScope(ServiceName.ANALYTICS)
public class AnalyticsImportController {

    private final SampleDataImportService importService;

    public AnalyticsImportController(SampleDataImportService importService) {
        this.importService = importService;
    }

    @PostMapping("/import")
    public ResponseEntity<ImportResult> importSampleData() {
        ImportResult result = importService.importFromConfiguredPath();
        return ResponseEntity.ok(result);
    }
}
