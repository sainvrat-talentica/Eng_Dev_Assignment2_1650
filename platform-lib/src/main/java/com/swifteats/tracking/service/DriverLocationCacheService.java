package com.swifteats.tracking.service;

import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swifteats.tracking.config.TrackingProperties;
import com.swifteats.tracking.dto.DriverLocationSnapshot;
import com.swifteats.tracking.dto.GpsLocationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@ServiceScope({ServiceName.ORDER, ServiceName.BACKEND})
public class DriverLocationCacheService {

    private static final Logger log = LoggerFactory.getLogger(DriverLocationCacheService.class);
    private static final String DRIVER_LOCATION_PREFIX = "driver:";
    private static final String DRIVER_LOCATION_SUFFIX = ":location";
    private static final String ORDER_DRIVER_PREFIX = "order:";
    private static final String ORDER_DRIVER_SUFFIX = ":driver";
    public static final String DRIVER_CHANNEL_PREFIX = "channel:driver:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final TrackingProperties properties;

    public DriverLocationCacheService(
            @Autowired(required = false) StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            TrackingProperties properties) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public void storeHotLocation(GpsLocationEvent event) {
        if (!isAvailable()) {
            return;
        }
        try {
            DriverLocationSnapshot snapshot = toSnapshot(event);
            String json = objectMapper.writeValueAsString(snapshot);
            redisTemplate.opsForValue().set(
                    driverLocationKey(event.driverId()),
                    json,
                    properties.getLocationCacheTtl());
            redisTemplate.convertAndSend(driverChannel(event.driverId()), json);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to cache GPS for driver {}: {}", event.driverId(), ex.getMessage());
        } catch (Exception ex) {
            log.warn("Redis GPS write failed for driver {}: {}", event.driverId(), ex.getMessage());
        }
    }

    public Optional<DriverLocationSnapshot> getLatestLocation(UUID driverId) {
        if (!isAvailable()) {
            return Optional.empty();
        }
        try {
            String json = redisTemplate.opsForValue().get(driverLocationKey(driverId));
            if (json == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, DriverLocationSnapshot.class));
        } catch (Exception ex) {
            log.warn("Redis GPS read failed for driver {}: {}", driverId, ex.getMessage());
            return Optional.empty();
        }
    }

    public void cacheOrderDriver(UUID orderId, UUID driverId) {
        if (!isAvailable()) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(
                    orderDriverKey(orderId),
                    driverId.toString(),
                    properties.getLocationCacheTtl());
        } catch (Exception ex) {
            log.warn("Failed to cache order-driver mapping for {}: {}", orderId, ex.getMessage());
        }
    }

    public Optional<UUID> getCachedOrderDriver(UUID orderId) {
        if (!isAvailable()) {
            return Optional.empty();
        }
        try {
            String value = redisTemplate.opsForValue().get(orderDriverKey(orderId));
            return value == null ? Optional.empty() : Optional.of(UUID.fromString(value));
        } catch (Exception ex) {
            log.warn("Failed to read order-driver mapping for {}: {}", orderId, ex.getMessage());
            return Optional.empty();
        }
    }

    public static String driverChannel(UUID driverId) {
        return DRIVER_CHANNEL_PREFIX + driverId;
    }

    public static String driverLocationKey(UUID driverId) {
        return DRIVER_LOCATION_PREFIX + driverId + DRIVER_LOCATION_SUFFIX;
    }

    private static String orderDriverKey(UUID orderId) {
        return ORDER_DRIVER_PREFIX + orderId + ORDER_DRIVER_SUFFIX;
    }

    private boolean isAvailable() {
        return redisTemplate != null;
    }

    private DriverLocationSnapshot toSnapshot(GpsLocationEvent event) {
        return new DriverLocationSnapshot(
                event.driverId(),
                event.orderId(),
                event.latitude(),
                event.longitude(),
                event.heading(),
                event.timestamp());
    }
}
