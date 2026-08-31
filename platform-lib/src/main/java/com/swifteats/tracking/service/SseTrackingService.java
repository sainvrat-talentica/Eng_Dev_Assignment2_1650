package com.swifteats.tracking.service;

import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swifteats.tracking.config.TrackingProperties;
import com.swifteats.tracking.dto.DriverLocationSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Service
@ServiceScope(ServiceName.BACKEND)
public class SseTrackingService {

    private static final Logger log = LoggerFactory.getLogger(SseTrackingService.class);

    private final TrackingProperties properties;
    private final DriverLocationCacheService locationCacheService;
    private final ObjectMapper objectMapper;
    private final RedisMessageListenerContainer listenerContainer;

    private final Map<UUID, SseEmitter> emittersByOrder = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> driverByOrder = new ConcurrentHashMap<>();
    private final Map<UUID, CopyOnWriteArraySet<UUID>> ordersByDriver = new ConcurrentHashMap<>();
    private final Map<UUID, MessageListener> listenersByDriver = new ConcurrentHashMap<>();

    public SseTrackingService(
            TrackingProperties properties,
            DriverLocationCacheService locationCacheService,
            ObjectMapper objectMapper,
            @Autowired(required = false) RedisMessageListenerContainer listenerContainer) {
        this.properties = properties;
        this.locationCacheService = locationCacheService;
        this.objectMapper = objectMapper;
        this.listenerContainer = listenerContainer;
    }

    public SseEmitter subscribe(UUID orderId, UUID driverId) {
        SseEmitter emitter = new SseEmitter(properties.getSseTimeoutMs());
        emittersByOrder.put(orderId, emitter);
        driverByOrder.put(orderId, driverId);
        ordersByDriver.computeIfAbsent(driverId, ignored -> new CopyOnWriteArraySet<>()).add(orderId);

        emitter.onCompletion(() -> cleanup(orderId));
        emitter.onTimeout(() -> cleanup(orderId));
        emitter.onError(ex -> cleanup(orderId));

        registerDriverListener(driverId);
        locationCacheService.getLatestLocation(driverId)
                .ifPresent(snapshot -> sendLocation(emitter, snapshot.withOrderId(orderId)));
        return emitter;
    }

    @Scheduled(fixedDelayString = "${swifteats.tracking.sse-heartbeat-ms:15000}")
    public void sendHeartbeats() {
        for (Map.Entry<UUID, SseEmitter> entry : emittersByOrder.entrySet()) {
            try {
                entry.getValue().send(SseEmitter.event().name("ping").data("{}"));
            } catch (Exception ex) {
                cleanup(entry.getKey());
            }
        }
    }

    private void registerDriverListener(UUID driverId) {
        if (listenerContainer == null || listenersByDriver.containsKey(driverId)) {
            return;
        }
        MessageListener listener = (message, pattern) -> onDriverLocationMessage(
                driverId, new String(message.getBody(), StandardCharsets.UTF_8));
        listenerContainer.addMessageListener(listener, new ChannelTopic(DriverLocationCacheService.driverChannel(driverId)));
        listenersByDriver.put(driverId, listener);
    }

    private void onDriverLocationMessage(UUID driverId, String payload) {
        CopyOnWriteArraySet<UUID> orderIds = ordersByDriver.get(driverId);
        if (orderIds == null || orderIds.isEmpty()) {
            return;
        }
        try {
            DriverLocationSnapshot snapshot = objectMapper.readValue(payload, DriverLocationSnapshot.class);
            for (UUID orderId : orderIds) {
                SseEmitter emitter = emittersByOrder.get(orderId);
                if (emitter != null) {
                    sendLocation(emitter, snapshot.withOrderId(orderId));
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to fan-out GPS update for driver {}: {}", driverId, ex.getMessage());
        }
    }

    private void sendLocation(SseEmitter emitter, DriverLocationSnapshot snapshot) {
        try {
            emitter.send(SseEmitter.event()
                    .name("location-update")
                    .data(snapshot));
        } catch (IOException ex) {
            log.debug("SSE client disconnected: {}", ex.getMessage());
        }
    }

    private void cleanup(UUID orderId) {
        emittersByOrder.remove(orderId);
        UUID driverId = driverByOrder.remove(orderId);
        if (driverId == null) {
            return;
        }
        CopyOnWriteArraySet<UUID> orders = ordersByDriver.get(driverId);
        if (orders != null) {
            orders.remove(orderId);
            if (orders.isEmpty()) {
                ordersByDriver.remove(driverId);
                removeDriverListener(driverId);
            }
        }
    }

    private void removeDriverListener(UUID driverId) {
        MessageListener listener = listenersByDriver.remove(driverId);
        if (listener != null && listenerContainer != null) {
            listenerContainer.removeMessageListener(listener);
        }
    }
}
