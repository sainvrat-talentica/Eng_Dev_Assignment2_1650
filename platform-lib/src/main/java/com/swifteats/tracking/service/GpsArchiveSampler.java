package com.swifteats.tracking.service;

import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;

import com.swifteats.tracking.config.TrackingProperties;
import com.swifteats.tracking.dto.GpsLocationEvent;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@ServiceScope(ServiceName.BACKEND)
public class GpsArchiveSampler {

    private final TrackingProperties properties;
    private final Map<UUID, Instant> lastArchivedAt = new ConcurrentHashMap<>();
    private final Map<UUID, AtomicInteger> orderEventCounters = new ConcurrentHashMap<>();

    public GpsArchiveSampler(TrackingProperties properties) {
        this.properties = properties;
    }

    public boolean shouldArchive(GpsLocationEvent event) {
        if (event.orderId() != null) {
            int count = orderEventCounters
                    .computeIfAbsent(event.driverId(), ignored -> new AtomicInteger())
                    .incrementAndGet();
            if (count % 5 == 0) {
                lastArchivedAt.put(event.driverId(), event.timestamp());
                return true;
            }
            return false;
        }

        Instant last = lastArchivedAt.get(event.driverId());
        if (last == null) {
            lastArchivedAt.put(event.driverId(), event.timestamp());
            return true;
        }
        long elapsed = Duration.between(last, event.timestamp()).getSeconds();
        if (elapsed >= properties.getArchiveSampleIntervalSec()) {
            lastArchivedAt.put(event.driverId(), event.timestamp());
            return true;
        }
        return false;
    }
}
