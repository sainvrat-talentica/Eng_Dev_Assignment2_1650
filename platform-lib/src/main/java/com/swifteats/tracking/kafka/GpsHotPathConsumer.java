package com.swifteats.tracking.kafka;

import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;

import com.swifteats.tracking.dto.GpsLocationEvent;
import com.swifteats.tracking.entity.DriverLocationArchive;
import com.swifteats.tracking.repository.DriverLocationArchiveRepository;
import com.swifteats.tracking.service.GpsArchiveSampler;
import com.swifteats.tracking.service.GpsHotPathProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "swifteats.tracking.kafka-enabled", havingValue = "true", matchIfMissing = true)
@ServiceScope(ServiceName.BACKEND)
public class GpsHotPathConsumer {

    private static final Logger log = LoggerFactory.getLogger(GpsHotPathConsumer.class);

    private final GpsHotPathProcessor hotPathProcessor;

    public GpsHotPathConsumer(GpsHotPathProcessor hotPathProcessor) {
        this.hotPathProcessor = hotPathProcessor;
    }

    @KafkaListener(topics = "${swifteats.tracking.gps-topic:gps.locations}", groupId = "gps-hot-path")
    public void consume(GpsLocationEvent event) {
        hotPathProcessor.process(event);
        log.trace("Hot-path cached GPS for driver {}", event.driverId());
    }
}
