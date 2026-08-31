package com.swifteats.tracking.kafka;

import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;

import com.swifteats.tracking.dto.GpsLocationEvent;
import com.swifteats.tracking.entity.DriverLocationArchive;
import com.swifteats.tracking.repository.DriverLocationArchiveRepository;
import com.swifteats.tracking.service.GpsArchiveSampler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "swifteats.tracking.kafka-enabled", havingValue = "true", matchIfMissing = true)
@ServiceScope(ServiceName.BACKEND)
public class GpsArchiveConsumer {

    private static final Logger log = LoggerFactory.getLogger(GpsArchiveConsumer.class);

    private final GpsArchiveSampler sampler;
    private final DriverLocationArchiveRepository archiveRepository;

    public GpsArchiveConsumer(GpsArchiveSampler sampler, DriverLocationArchiveRepository archiveRepository) {
        this.sampler = sampler;
        this.archiveRepository = archiveRepository;
    }

    @KafkaListener(topics = "${swifteats.tracking.gps-topic:gps.locations}", groupId = "gps-archive")
    @Transactional
    public void archive(GpsLocationEvent event) {
        if (!sampler.shouldArchive(event)) {
            return;
        }
        DriverLocationArchive archive = new DriverLocationArchive();
        archive.setDriverId(event.driverId());
        archive.setOrderId(event.orderId());
        archive.setLatitude(event.latitude());
        archive.setLongitude(event.longitude());
        archive.setHeading(event.heading());
        archive.setRecordedAt(event.timestamp());
        archiveRepository.save(archive);
        log.trace("Archived GPS for driver {}", event.driverId());
    }
}
