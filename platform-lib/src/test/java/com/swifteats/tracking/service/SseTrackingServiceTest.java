package com.swifteats.tracking.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.swifteats.tracking.config.TrackingProperties;
import com.swifteats.tracking.dto.DriverLocationSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SseTrackingServiceTest {

    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID DRIVER_ID = UUID.randomUUID();

    @Mock
    private DriverLocationCacheService locationCacheService;
    @Mock
    private RedisMessageListenerContainer listenerContainer;

    private TrackingProperties properties;
    private ObjectMapper objectMapper;
    private SseTrackingService service;

    @BeforeEach
    void setUp() {
        properties = new TrackingProperties();
        properties.setSseTimeoutMs(60_000L);
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        service = new SseTrackingService(properties, locationCacheService, objectMapper, null);
    }

    @Test
    void subscribe_createsEmitterAndRegistersOrder() {
        when(locationCacheService.getLatestLocation(DRIVER_ID)).thenReturn(Optional.empty());

        SseEmitter emitter = service.subscribe(ORDER_ID, DRIVER_ID);

        assertThat(emitter).isNotNull();
        assertThat(emitterByOrder()).containsKey(ORDER_ID);
        assertThat(driverByOrder()).containsEntry(ORDER_ID, DRIVER_ID);
    }

    @Test
    void subscribe_sendsCachedLocationWhenPresent() {
        DriverLocationSnapshot snapshot = new DriverLocationSnapshot(
                DRIVER_ID, null, BigDecimal.ONE, BigDecimal.TEN, null, Instant.now());
        when(locationCacheService.getLatestLocation(DRIVER_ID)).thenReturn(Optional.of(snapshot));

        SseEmitter emitter = service.subscribe(ORDER_ID, DRIVER_ID);

        assertThat(emitter).isNotNull();
        assertThat(emitterByOrder()).containsKey(ORDER_ID);
    }

    @Test
    void sendHeartbeats_doesNotThrowWithActiveEmitter() {
        when(locationCacheService.getLatestLocation(DRIVER_ID)).thenReturn(Optional.empty());
        service.subscribe(ORDER_ID, DRIVER_ID);

        service.sendHeartbeats();

        assertThat(emitterByOrder()).containsKey(ORDER_ID);
    }

    @Test
    void cleanup_removesEmitterAndDriverMappings() {
        when(locationCacheService.getLatestLocation(DRIVER_ID)).thenReturn(Optional.empty());
        service.subscribe(ORDER_ID, DRIVER_ID);
        assertThat(emitterByOrder()).containsKey(ORDER_ID);

        ReflectionTestUtils.invokeMethod(service, "cleanup", ORDER_ID);

        assertThat(emitterByOrder()).doesNotContainKey(ORDER_ID);
        assertThat(driverByOrder()).doesNotContainKey(ORDER_ID);
    }

    @Test
    void subscribe_registersRedisListenerWhenContainerPresent() {
        service = new SseTrackingService(properties, locationCacheService, objectMapper, listenerContainer);
        when(locationCacheService.getLatestLocation(DRIVER_ID)).thenReturn(Optional.empty());

        service.subscribe(ORDER_ID, DRIVER_ID);

        verify(listenerContainer).addMessageListener(any(MessageListener.class), any(ChannelTopic.class));
    }

    @Test
    void onDriverLocationMessage_fansOutUpdatesToSubscribedOrders() throws Exception {
        service = new SseTrackingService(properties, locationCacheService, objectMapper, listenerContainer);
        when(locationCacheService.getLatestLocation(DRIVER_ID)).thenReturn(Optional.empty());
        service.subscribe(ORDER_ID, DRIVER_ID);

        ArgumentCaptor<MessageListener> listenerCaptor = ArgumentCaptor.forClass(MessageListener.class);
        verify(listenerContainer).addMessageListener(listenerCaptor.capture(), any(ChannelTopic.class));

        DriverLocationSnapshot snapshot = new DriverLocationSnapshot(
                DRIVER_ID, null, BigDecimal.ONE, BigDecimal.TEN, null, Instant.now());
        Message message = mock(Message.class);
        when(message.getBody()).thenReturn(objectMapper.writeValueAsString(snapshot).getBytes(StandardCharsets.UTF_8));

        listenerCaptor.getValue().onMessage(message, null);

        assertThat(emitterByOrder()).containsKey(ORDER_ID);
    }

    @SuppressWarnings("unchecked")
    private Map<UUID, SseEmitter> emitterByOrder() {
        return (Map<UUID, SseEmitter>) ReflectionTestUtils.getField(service, "emittersByOrder");
    }

    @SuppressWarnings("unchecked")
    private Map<UUID, UUID> driverByOrder() {
        return (Map<UUID, UUID>) ReflectionTestUtils.getField(service, "driverByOrder");
    }
}
