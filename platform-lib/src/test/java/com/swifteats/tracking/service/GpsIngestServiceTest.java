package com.swifteats.tracking.service;

import com.swifteats.common.exception.ResourceNotFoundException;
import com.swifteats.tracking.config.TrackingConfig;
import com.swifteats.tracking.config.TrackingProperties;
import com.swifteats.tracking.dto.GpsLocationEvent;
import com.swifteats.tracking.dto.LocationUpdateRequest;
import com.swifteats.tracking.repository.DriverRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GpsIngestServiceTest {

    private static final UUID DRIVER_ID = UUID.fromString("55555555-5555-5555-5555-555555555501");
    private static final UUID ORDER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final Instant TIMESTAMP = Instant.parse("2025-08-15T12:00:00Z");

    @Mock
    private DriverRepository driverRepository;
    @Mock
    private TrackingProperties trackingProperties;
    @Mock
    private GpsHotPathProcessor hotPathProcessor;
    @Mock
    private KafkaTemplate<String, GpsLocationEvent> kafkaTemplate;

    @Test
    void ingest_throwsWhenDriverNotFound() {
        GpsIngestService service = new GpsIngestService(
                driverRepository, trackingProperties, hotPathProcessor, kafkaTemplate);
        LocationUpdateRequest request = locationRequest(TIMESTAMP);

        when(driverRepository.existsById(DRIVER_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.ingest(DRIVER_ID, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Driver not found");

        verify(kafkaTemplate, never()).send(
                eq(TrackingConfig.GPS_LOCATIONS_TOPIC), eq(DRIVER_ID.toString()), org.mockito.ArgumentMatchers.any());
        verify(hotPathProcessor, never()).process(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void ingest_publishesToKafkaWhenEnabled() {
        GpsIngestService service = new GpsIngestService(
                driverRepository, trackingProperties, hotPathProcessor, kafkaTemplate);
        LocationUpdateRequest request = locationRequest(TIMESTAMP);

        when(driverRepository.existsById(DRIVER_ID)).thenReturn(true);
        when(trackingProperties.isKafkaEnabled()).thenReturn(true);

        service.ingest(DRIVER_ID, request);

        ArgumentCaptor<GpsLocationEvent> eventCaptor = ArgumentCaptor.forClass(GpsLocationEvent.class);
        verify(kafkaTemplate).send(
                eq(TrackingConfig.GPS_LOCATIONS_TOPIC), eq(DRIVER_ID.toString()), eventCaptor.capture());
        verify(hotPathProcessor, never()).process(org.mockito.ArgumentMatchers.any());

        GpsLocationEvent event = eventCaptor.getValue();
        assertThat(event.driverId()).isEqualTo(DRIVER_ID);
        assertThat(event.latitude()).isEqualByComparingTo("18.5204");
        assertThat(event.longitude()).isEqualByComparingTo("73.8567");
        assertThat(event.heading()).isEqualByComparingTo("90.0");
        assertThat(event.timestamp()).isEqualTo(TIMESTAMP);
        assertThat(event.orderId()).isEqualTo(ORDER_ID);
    }

    @Test
    void ingest_processesInlineWhenKafkaDisabled() {
        GpsIngestService service = new GpsIngestService(
                driverRepository, trackingProperties, hotPathProcessor, kafkaTemplate);
        LocationUpdateRequest request = new LocationUpdateRequest(
                new BigDecimal("18.5204"),
                new BigDecimal("73.8567"),
                new BigDecimal("90.0"),
                null,
                ORDER_ID);

        when(driverRepository.existsById(DRIVER_ID)).thenReturn(true);
        when(trackingProperties.isKafkaEnabled()).thenReturn(false);

        service.ingest(DRIVER_ID, request);

        ArgumentCaptor<GpsLocationEvent> eventCaptor = ArgumentCaptor.forClass(GpsLocationEvent.class);
        verify(hotPathProcessor).process(eventCaptor.capture());
        verify(kafkaTemplate, never()).send(
                eq(TrackingConfig.GPS_LOCATIONS_TOPIC), eq(DRIVER_ID.toString()), org.mockito.ArgumentMatchers.any());

        GpsLocationEvent event = eventCaptor.getValue();
        assertThat(event.driverId()).isEqualTo(DRIVER_ID);
        assertThat(event.orderId()).isEqualTo(ORDER_ID);
        assertThat(event.timestamp()).isNotNull();
    }

    @Test
    void ingest_processesInlineWhenKafkaTemplateMissing() {
        GpsIngestService service = new GpsIngestService(
                driverRepository, trackingProperties, hotPathProcessor, null);
        LocationUpdateRequest request = locationRequest(TIMESTAMP);

        when(driverRepository.existsById(DRIVER_ID)).thenReturn(true);
        when(trackingProperties.isKafkaEnabled()).thenReturn(true);

        service.ingest(DRIVER_ID, request);

        verify(hotPathProcessor).process(org.mockito.ArgumentMatchers.any(GpsLocationEvent.class));
    }

    private static LocationUpdateRequest locationRequest(Instant timestamp) {
        return new LocationUpdateRequest(
                new BigDecimal("18.5204"),
                new BigDecimal("73.8567"),
                new BigDecimal("90.0"),
                timestamp,
                ORDER_ID);
    }
}
