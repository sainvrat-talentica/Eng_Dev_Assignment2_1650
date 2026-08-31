package com.swifteats.order.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.swifteats.common.security.InternalServiceAuthSupport;
import com.swifteats.gateway.config.ServiceRegistryProperties;
import com.swifteats.order.config.PaymentProperties;
import com.swifteats.order.dto.PaymentProcessMessage;
import com.swifteats.order.entity.OutboxEvent;
import com.swifteats.order.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Pageable;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxPollerTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;
    @Mock
    private PaymentProperties paymentProperties;
    @Mock
    private ServiceRegistryProperties serviceRegistry;
    @Mock
    private InternalServiceAuthSupport internalServiceAuthSupport;
    @Mock
    private RabbitTemplate rabbitTemplate;

    private OutboxPoller poller;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        poller = new OutboxPoller(
                outboxEventRepository,
                objectMapper,
                paymentProperties,
                serviceRegistry,
                RestClient.builder(),
                internalServiceAuthSupport,
                rabbitTemplate);
    }

    @Test
    void pollAndPublish_sendsToRabbitWhenMessagingEnabled() throws Exception {
        UUID orderId = UUID.randomUUID();
        PaymentProcessMessage message = new PaymentProcessMessage(orderId, BigDecimal.TEN);
        OutboxEvent event = new OutboxEvent();
        event.setId(UUID.randomUUID());
        event.setEventType("PaymentProcess");
        event.setPayload(objectMapper.writeValueAsString(message));
        event.setCreatedAt(Instant.now());

        when(outboxEventRepository.findUnpublished(any(Pageable.class))).thenReturn(List.of(event));
        when(paymentProperties.isMessagingEnabled()).thenReturn(true);

        poller.pollAndPublish();

        verify(rabbitTemplate).convertAndSend(
                eq(com.swifteats.order.config.PaymentMessagingConfig.PAYMENT_EXCHANGE),
                eq(com.swifteats.order.config.PaymentMessagingConfig.PAYMENT_ROUTING_KEY),
                any(PaymentProcessMessage.class));
    }

    @Test
    void pollAndPublish_skipsUnknownEventTypes() {
        OutboxEvent event = new OutboxEvent();
        event.setId(UUID.randomUUID());
        event.setEventType("Unknown");
        event.setPayload("{}");
        event.setCreatedAt(Instant.now());

        when(outboxEventRepository.findUnpublished(any(Pageable.class))).thenReturn(List.of(event));

        poller.pollAndPublish();

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(PaymentProcessMessage.class));
    }
}
