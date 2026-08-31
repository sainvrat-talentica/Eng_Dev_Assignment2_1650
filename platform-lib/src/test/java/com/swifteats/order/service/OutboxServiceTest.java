package com.swifteats.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swifteats.order.entity.OutboxEvent;
import com.swifteats.order.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

    private static final UUID AGGREGATE_ID = UUID.randomUUID();

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    private OutboxService outboxService;

    @BeforeEach
    void setUp() {
        outboxService = new OutboxService(outboxEventRepository, objectMapper);
    }

    @Test
    void enqueue_persistsSerializedEvent() throws Exception {
        Map<String, String> payload = Map.of("status", "PAID");
        when(objectMapper.writeValueAsString(payload)).thenReturn("{\"status\":\"PAID\"}");
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        outboxService.enqueue("order", AGGREGATE_ID, "PaymentCaptured", payload);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        OutboxEvent saved = captor.getValue();
        assertThat(saved.getAggregateType()).isEqualTo("order");
        assertThat(saved.getAggregateId()).isEqualTo(AGGREGATE_ID);
        assertThat(saved.getEventType()).isEqualTo("PaymentCaptured");
        assertThat(saved.getPayload()).isEqualTo("{\"status\":\"PAID\"}");
    }

    @Test
    void enqueue_wrapsSerializationFailure() throws Exception {
        doThrow(new JsonProcessingException("bad payload") {}).when(objectMapper).writeValueAsString(any());

        assertThatThrownBy(() -> outboxService.enqueue("order", AGGREGATE_ID, "Broken", Map.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to serialize outbox payload");
    }
}
