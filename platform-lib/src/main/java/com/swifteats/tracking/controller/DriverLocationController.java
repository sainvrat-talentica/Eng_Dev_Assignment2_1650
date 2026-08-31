package com.swifteats.tracking.controller;

import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;

import com.swifteats.common.config.OpenApiConfig;
import com.swifteats.tracking.dto.LocationAcceptedResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import com.swifteats.tracking.dto.LocationUpdateRequest;
import com.swifteats.tracking.service.GpsIngestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/drivers")
@SecurityRequirement(name = OpenApiConfig.DRIVER_API_KEY)
@ServiceScope(ServiceName.BACKEND)
public class DriverLocationController {

    private final GpsIngestService gpsIngestService;

    public DriverLocationController(GpsIngestService gpsIngestService) {
        this.gpsIngestService = gpsIngestService;
    }

    @PostMapping("/{driverId}/location")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public LocationAcceptedResponse ingestLocation(
            @PathVariable UUID driverId,
            @Valid @RequestBody LocationUpdateRequest request) {
        gpsIngestService.ingest(driverId, request);
        return new LocationAcceptedResponse(true);
    }
}
