package com.swifteats.tracking.controller;

import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;

import com.swifteats.common.config.OpenApiConfig;
import com.swifteats.common.security.RequestAuthAttributes;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import com.swifteats.tracking.dto.DriverLocationSnapshot;
import com.swifteats.tracking.service.SseTrackingService;
import com.swifteats.tracking.service.TrackingService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@SecurityRequirements({
        @SecurityRequirement(name = OpenApiConfig.CUSTOMER_ID),
        @SecurityRequirement(name = OpenApiConfig.CUSTOMER_API_KEY)
})
@ServiceScope(ServiceName.BACKEND)
public class TrackingController {

    private final TrackingService trackingService;
    private final SseTrackingService sseTrackingService;

    public TrackingController(TrackingService trackingService, SseTrackingService sseTrackingService) {
        this.trackingService = trackingService;
        this.sseTrackingService = sseTrackingService;
    }

    @GetMapping("/{orderId}/tracking")
    public DriverLocationSnapshot getTrackingSnapshot(HttpServletRequest httpRequest, @PathVariable UUID orderId) {
        UUID customerId = RequestAuthAttributes.customerId(httpRequest);
        return trackingService.getSnapshot(orderId, customerId);
    }

    @GetMapping(value = "/{orderId}/tracking/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamTracking(HttpServletRequest httpRequest, @PathVariable UUID orderId) {
        UUID customerId = RequestAuthAttributes.customerId(httpRequest);
        UUID driverId = trackingService.requireDriverId(orderId, customerId);
        return sseTrackingService.subscribe(orderId, driverId);
    }
}
