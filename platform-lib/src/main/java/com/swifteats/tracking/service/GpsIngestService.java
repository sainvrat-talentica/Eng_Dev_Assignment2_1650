package com.swifteats.tracking.service;

import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;

import com.swifteats.common.exception.ResourceNotFoundException;
import com.swifteats.tracking.config.TrackingConfig;
import com.swifteats.tracking.config.TrackingProperties;
import com.swifteats.tracking.dto.GpsLocationEvent;
import com.swifteats.tracking.dto.LocationUpdateRequest;
import com.swifteats.tracking.entity.Driver;
import com.swifteats.tracking.repository.DriverRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@ServiceScope(ServiceName.BACKEND)
public class GpsIngestService {

    private static final Logger log = LoggerFactory.getLogger(GpsIngestService.class);

    private final DriverRepository driverRepository;
    private final TrackingProperties trackingProperties;
    private final GpsHotPathProcessor hotPathProcessor;
    private final KafkaTemplate<String, GpsLocationEvent> kafkaTemplate;

    public GpsIngestService(
            DriverRepository driverRepository,
            TrackingProperties trackingProperties,
            GpsHotPathProcessor hotPathProcessor,
            @Autowired(required = false) KafkaTemplate<String, GpsLocationEvent> kafkaTemplate) {
        this.driverRepository = driverRepository;
        this.trackingProperties = trackingProperties;
        this.hotPathProcessor = hotPathProcessor;
        this.kafkaTemplate = kafkaTemplate;
    }

    public void ingest(UUID driverId, LocationUpdateRequest request) {
        if (!driverRepository.existsById(driverId)) {
            throw new ResourceNotFoundException("Driver not found");
        }

        GpsLocationEvent event = new GpsLocationEvent(
                driverId,
                request.latitude(),
                request.longitude(),
                request.heading(),
                request.timestamp() != null ? request.timestamp() : Instant.now(),
                request.orderId());

        if (trackingProperties.isKafkaEnabled() && kafkaTemplate != null) {
            kafkaTemplate.send(TrackingConfig.GPS_LOCATIONS_TOPIC, driverId.toString(), event);
            log.debug("Published GPS event for driver {}", driverId);
            return;
        }

        hotPathProcessor.process(event);
        log.debug("Processed GPS event inline for driver {} (Kafka disabled)", driverId);
    }
}
