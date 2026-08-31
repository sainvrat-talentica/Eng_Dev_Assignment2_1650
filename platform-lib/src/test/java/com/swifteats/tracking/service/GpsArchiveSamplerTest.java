package com.swifteats.tracking.service;

import com.swifteats.tracking.config.TrackingProperties;
import com.swifteats.tracking.dto.GpsLocationEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GpsArchiveSamplerTest {

    private GpsArchiveSampler sampler;
    private final UUID driverId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        TrackingProperties properties = new TrackingProperties();
        properties.setArchiveSampleIntervalSec(30);
        sampler = new GpsArchiveSampler(properties);
    }

    @Test
    void archivesFirstEventForDriver() {
        GpsLocationEvent event = event(Instant.parse("2026-08-21T10:00:00Z"), null);
        assertThat(sampler.shouldArchive(event)).isTrue();
    }

    @Test
    void archivesEveryFifthEventWhenOrderPresent() {
        UUID orderId = UUID.randomUUID();
        for (int i = 1; i <= 4; i++) {
            assertThat(sampler.shouldArchive(event(Instant.parse("2026-08-21T10:00:0" + i + "Z"), orderId)))
                    .isFalse();
        }
        assertThat(sampler.shouldArchive(event(Instant.parse("2026-08-21T10:00:05Z"), orderId)))
                .isTrue();
    }

    private GpsLocationEvent event(Instant timestamp, UUID orderId) {
        return new GpsLocationEvent(
                driverId,
                BigDecimal.valueOf(18.52),
                BigDecimal.valueOf(73.85),
                BigDecimal.ZERO,
                timestamp,
                orderId);
    }
}
