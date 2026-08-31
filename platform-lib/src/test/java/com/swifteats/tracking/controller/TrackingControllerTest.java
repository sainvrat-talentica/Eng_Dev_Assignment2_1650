package com.swifteats.tracking.controller;

import com.swifteats.common.security.RequestAuthAttributes;
import com.swifteats.tracking.dto.DriverLocationSnapshot;
import com.swifteats.tracking.service.SseTrackingService;
import com.swifteats.tracking.service.TrackingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TrackingControllerTest {

    private static final UUID CUSTOMER_ID = UUID.fromString("44444444-4444-4444-4444-444444444401");
    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID DRIVER_ID = UUID.randomUUID();

    @Mock
    private TrackingService trackingService;
    @Mock
    private SseTrackingService sseTrackingService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TrackingController(trackingService, sseTrackingService))
                .build();
    }

    @Test
    void getTrackingSnapshot_returnsLocation() throws Exception {
        DriverLocationSnapshot snapshot = new DriverLocationSnapshot(
                DRIVER_ID, ORDER_ID, BigDecimal.valueOf(18.52), BigDecimal.valueOf(73.85),
                BigDecimal.ZERO, Instant.parse("2026-08-21T18:00:00Z"));
        when(trackingService.getSnapshot(ORDER_ID, CUSTOMER_ID)).thenReturn(snapshot);

        mockMvc.perform(get("/api/v1/orders/{orderId}/tracking", ORDER_ID)
                        .requestAttr(RequestAuthAttributes.CUSTOMER_ID, CUSTOMER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.driverId").value(DRIVER_ID.toString()))
                .andExpect(jsonPath("$.latitude").value(18.52));
    }

    @Test
    void streamTracking_subscribesToSse() throws Exception {
        SseEmitter emitter = new SseEmitter();
        when(trackingService.requireDriverId(ORDER_ID, CUSTOMER_ID)).thenReturn(DRIVER_ID);
        when(sseTrackingService.subscribe(ORDER_ID, DRIVER_ID)).thenReturn(emitter);

        mockMvc.perform(get("/api/v1/orders/{orderId}/tracking/stream", ORDER_ID)
                        .requestAttr(RequestAuthAttributes.CUSTOMER_ID, CUSTOMER_ID))
                .andExpect(status().isOk());

        verify(sseTrackingService).subscribe(ORDER_ID, DRIVER_ID);
    }
}
