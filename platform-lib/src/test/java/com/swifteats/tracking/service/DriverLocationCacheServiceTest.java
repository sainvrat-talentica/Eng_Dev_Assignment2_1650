package com.swifteats.tracking.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swifteats.tracking.config.TrackingProperties;
import com.swifteats.tracking.dto.DriverLocationSnapshot;
import com.swifteats.tracking.dto.GpsLocationEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverLocationCacheServiceTest {

    private static final UUID DRIVER_ID = UUID.fromString("55555555-5555-5555-5555-555555555501");
    private static final UUID ORDER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final Instant TIMESTAMP = Instant.parse("2025-08-15T12:00:00Z");
    private static final Duration TTL = Duration.ofMinutes(30);

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private TrackingProperties properties;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    @Test
    void storeHotLocation_writesSnapshotAndPublishesChannel() throws Exception {
        DriverLocationCacheService service = serviceWithRedisAndTtl();
        GpsLocationEvent event = sampleEvent();

        service.storeHotLocation(event);

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(
                eq(DriverLocationCacheService.driverLocationKey(DRIVER_ID)),
                jsonCaptor.capture(),
                eq(TTL));
        verify(redisTemplate).convertAndSend(
                eq(DriverLocationCacheService.driverChannel(DRIVER_ID)),
                eq(jsonCaptor.getValue()));

        DriverLocationSnapshot snapshot = objectMapper.readValue(jsonCaptor.getValue(), DriverLocationSnapshot.class);
        assertThat(snapshot.driverId()).isEqualTo(DRIVER_ID);
        assertThat(snapshot.orderId()).isEqualTo(ORDER_ID);
        assertThat(snapshot.latitude()).isEqualByComparingTo("18.5204");
        assertThat(snapshot.longitude()).isEqualByComparingTo("73.8567");
        assertThat(snapshot.heading()).isEqualByComparingTo("90.0");
        assertThat(snapshot.timestamp()).isEqualTo(TIMESTAMP);
    }

    @Test
    void getLatestLocation_returnsSnapshotWhenPresent() throws Exception {
        DriverLocationCacheService service = serviceWithRedis();
        DriverLocationSnapshot snapshot = new DriverLocationSnapshot(
                DRIVER_ID, ORDER_ID, new BigDecimal("18.5204"), new BigDecimal("73.8567"),
                new BigDecimal("90.0"), TIMESTAMP);
        String json = objectMapper.writeValueAsString(snapshot);

        when(valueOperations.get(DriverLocationCacheService.driverLocationKey(DRIVER_ID))).thenReturn(json);

        Optional<DriverLocationSnapshot> result = service.getLatestLocation(DRIVER_ID);

        assertThat(result).isPresent();
        assertThat(result.get().driverId()).isEqualTo(DRIVER_ID);
        assertThat(result.get().orderId()).isEqualTo(ORDER_ID);
    }

    @Test
    void getLatestLocation_returnsEmptyWhenMissing() {
        DriverLocationCacheService service = serviceWithRedis();
        when(valueOperations.get(DriverLocationCacheService.driverLocationKey(DRIVER_ID))).thenReturn(null);

        Optional<DriverLocationSnapshot> result = service.getLatestLocation(DRIVER_ID);

        assertThat(result).isEmpty();
    }

    @Test
    void cacheOrderDriver_storesMappingWithTtl() {
        DriverLocationCacheService service = serviceWithRedisAndTtl();

        service.cacheOrderDriver(ORDER_ID, DRIVER_ID);

        verify(valueOperations).set(
                eq("order:" + ORDER_ID + ":driver"),
                eq(DRIVER_ID.toString()),
                eq(TTL));
    }

    @Test
    void getCachedOrderDriver_returnsDriverIdWhenPresent() {
        DriverLocationCacheService service = serviceWithRedis();
        when(valueOperations.get("order:" + ORDER_ID + ":driver")).thenReturn(DRIVER_ID.toString());

        Optional<UUID> result = service.getCachedOrderDriver(ORDER_ID);

        assertThat(result).contains(DRIVER_ID);
    }

    @Test
    void getCachedOrderDriver_returnsEmptyWhenMissing() {
        DriverLocationCacheService service = serviceWithRedis();
        when(valueOperations.get("order:" + ORDER_ID + ":driver")).thenReturn(null);

        Optional<UUID> result = service.getCachedOrderDriver(ORDER_ID);

        assertThat(result).isEmpty();
    }

    @Test
    void operationsAreNoOpWhenRedisUnavailable() {
        DriverLocationCacheService unavailable = new DriverLocationCacheService(null, objectMapper, properties);

        unavailable.storeHotLocation(sampleEvent());
        assertThat(unavailable.getLatestLocation(DRIVER_ID)).isEmpty();
        unavailable.cacheOrderDriver(ORDER_ID, DRIVER_ID);
        assertThat(unavailable.getCachedOrderDriver(ORDER_ID)).isEmpty();

        verify(valueOperations, never()).set(anyString(), anyString(), eq(TTL));
    }

    @Test
    void keyHelpers_formatExpectedRedisKeys() {
        assertThat(DriverLocationCacheService.driverLocationKey(DRIVER_ID))
                .isEqualTo("driver:" + DRIVER_ID + ":location");
        assertThat(DriverLocationCacheService.driverChannel(DRIVER_ID))
                .isEqualTo("channel:driver:" + DRIVER_ID);
    }

    private DriverLocationCacheService serviceWithRedis() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        return new DriverLocationCacheService(redisTemplate, objectMapper, properties);
    }

    private DriverLocationCacheService serviceWithRedisAndTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(properties.getLocationCacheTtl()).thenReturn(TTL);
        return new DriverLocationCacheService(redisTemplate, objectMapper, properties);
    }

    private static GpsLocationEvent sampleEvent() {
        return new GpsLocationEvent(
                DRIVER_ID,
                new BigDecimal("18.5204"),
                new BigDecimal("73.8567"),
                new BigDecimal("90.0"),
                TIMESTAMP,
                ORDER_ID);
    }
}
