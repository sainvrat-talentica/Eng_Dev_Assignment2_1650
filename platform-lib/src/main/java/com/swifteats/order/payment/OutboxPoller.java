package com.swifteats.order.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;
import com.swifteats.common.security.InternalServiceAuthSupport;
import com.swifteats.gateway.config.ServiceRegistryProperties;
import com.swifteats.order.config.PaymentMessagingConfig;
import com.swifteats.order.config.PaymentProperties;
import com.swifteats.order.dto.PaymentProcessMessage;
import com.swifteats.order.entity.OutboxEvent;
import com.swifteats.order.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;

@Component
@ServiceScope(ServiceName.ORDER)
public class OutboxPoller {

    private static final Logger log = LoggerFactory.getLogger(OutboxPoller.class);

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final PaymentProperties paymentProperties;
    private final ServiceRegistryProperties serviceRegistry;
    private final RestClient restClient;
    private final InternalServiceAuthSupport internalServiceAuthSupport;
    private final RabbitTemplate rabbitTemplate;

    public OutboxPoller(
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper,
            PaymentProperties paymentProperties,
            ServiceRegistryProperties serviceRegistry,
            RestClient.Builder restClientBuilder,
            InternalServiceAuthSupport internalServiceAuthSupport,
            @Autowired(required = false) RabbitTemplate rabbitTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.paymentProperties = paymentProperties;
        this.serviceRegistry = serviceRegistry;
        this.restClient = restClientBuilder.build();
        this.internalServiceAuthSupport = internalServiceAuthSupport;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(fixedDelayString = "${swifteats.outbox.poll-interval-ms:1000}")
    @Transactional
    public void pollAndPublish() {
        List<OutboxEvent> batch = outboxEventRepository.findUnpublished(PageRequest.of(0, 100));
        for (OutboxEvent event : batch) {
            try {
                dispatch(event);
                event.setPublishedAt(Instant.now());
            } catch (Exception ex) {
                log.warn("Failed to publish outbox event {}: {}", event.getId(), ex.getMessage());
            }
        }
    }

    private void dispatch(OutboxEvent event) throws Exception {
        if ("PaymentProcess".equals(event.getEventType())) {
            PaymentProcessMessage message = objectMapper.readValue(event.getPayload(), PaymentProcessMessage.class);
            if (paymentProperties.isMessagingEnabled() && rabbitTemplate != null) {
                rabbitTemplate.convertAndSend(
                        PaymentMessagingConfig.PAYMENT_EXCHANGE,
                        PaymentMessagingConfig.PAYMENT_ROUTING_KEY,
                        message);
            } else {
                String baseUrl = serviceRegistry.getPayment().getBaseUrl();
                internalServiceAuthSupport.authorize(restClient.post()
                        .uri(baseUrl + "/internal/v1/payments/process")
                        .body(message))
                        .retrieve()
                        .toBodilessEntity();
            }
            return;
        }
        log.debug("Outbox event {} ({}) recorded; no external publisher configured", event.getId(), event.getEventType());
    }
}
