package com.swifteats.tracking.simulator;

import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;

import com.swifteats.tracking.config.TrackingProperties;
import com.swifteats.tracking.dto.LocationUpdateRequest;
import com.swifteats.tracking.entity.Driver;
import com.swifteats.tracking.repository.DriverRepository;
import com.swifteats.tracking.service.GpsIngestService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Local demo simulator — up to 50 drivers, ~10 events/sec at max configuration.
 * Posts GPS updates through the same ingest path as real driver apps.
 */
@Component
@ConditionalOnProperty(name = "swifteats.tracking.simulator-enabled", havingValue = "true")
@ServiceScope(ServiceName.BACKEND)
public class DriverGpsSimulator {

    private static final Logger log = LoggerFactory.getLogger(DriverGpsSimulator.class);
    private static final BigDecimal PUNE_LAT = new BigDecimal("18.5204");
    private static final BigDecimal PUNE_LON = new BigDecimal("73.8567");

    private final DriverRepository driverRepository;
    private final GpsIngestService gpsIngestService;
    private final TrackingProperties properties;
    private final Map<UUID, SimState> states = new ConcurrentHashMap<>();

    public DriverGpsSimulator(
            DriverRepository driverRepository,
            GpsIngestService gpsIngestService,
            TrackingProperties properties) {
        this.driverRepository = driverRepository;
        this.gpsIngestService = gpsIngestService;
        this.properties = properties;
    }

    @PostConstruct
    void init() {
        List<Driver> drivers = driverRepository.findAll();
        int limit = Math.min(properties.getSimulatorDriverCount(), drivers.size());
        for (int i = 0; i < limit; i++) {
            Driver driver = drivers.get(i);
            states.put(driver.getId(), new SimState(
                    PUNE_LAT.add(BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble(-0.05, 0.05)))
                            .setScale(6, RoundingMode.HALF_UP),
                    PUNE_LON.add(BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble(-0.05, 0.05)))
                            .setScale(6, RoundingMode.HALF_UP),
                    driver.getCurrentOrderId()));
        }
        log.info("GPS simulator initialized for {} drivers", states.size());
    }

    @Scheduled(fixedDelayString = "${swifteats.tracking.simulator-interval-ms:5000}")
    void tick() {
        if (states.isEmpty()) {
            return;
        }
        for (Map.Entry<UUID, SimState> entry : states.entrySet()) {
            UUID driverId = entry.getKey();
            SimState state = entry.getValue();
            state.latitude = drift(state.latitude);
            state.longitude = drift(state.longitude);
            state.heading = BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble(0, 360))
                    .setScale(1, RoundingMode.HALF_UP);

            LocationUpdateRequest request = new LocationUpdateRequest(
                    state.latitude,
                    state.longitude,
                    state.heading,
                    null,
                    state.orderId);
            gpsIngestService.ingest(driverId, request);
        }
    }

    private BigDecimal drift(BigDecimal coordinate) {
        double delta = ThreadLocalRandom.current().nextDouble(-0.001, 0.001);
        return coordinate.add(BigDecimal.valueOf(delta)).setScale(6, RoundingMode.HALF_UP);
    }

    private static final class SimState {
        private BigDecimal latitude;
        private BigDecimal longitude;
        private BigDecimal heading;
        private UUID orderId;

        private SimState(BigDecimal latitude, BigDecimal longitude, UUID orderId) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.orderId = orderId;
        }
    }
}
