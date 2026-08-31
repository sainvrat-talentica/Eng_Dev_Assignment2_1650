package com.swifteats.order.client;

import com.swifteats.common.domain.OrderStatus;
import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;
import com.swifteats.common.security.InternalServiceAuthSupport;
import com.swifteats.gateway.config.ServiceRegistryProperties;
import com.swifteats.order.dto.OrderTransitionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
@ServiceScope({ServiceName.PAYMENT, ServiceName.REFUND})
public class HttpOrderInternalClient implements OrderInternalClient {

    private static final Logger log = LoggerFactory.getLogger(HttpOrderInternalClient.class);

    private final RestClient restClient;
    private final ServiceRegistryProperties serviceRegistry;
    private final InternalServiceAuthSupport internalServiceAuthSupport;

    public HttpOrderInternalClient(
            RestClient.Builder restClientBuilder,
            ServiceRegistryProperties serviceRegistry,
            InternalServiceAuthSupport internalServiceAuthSupport) {
        this.restClient = restClientBuilder.build();
        this.serviceRegistry = serviceRegistry;
        this.internalServiceAuthSupport = internalServiceAuthSupport;
    }

    @Override
    public void transition(UUID orderId, OrderStatus newStatus, String changedBy, String reason) {
        String baseUrl = serviceRegistry.getOrder().getBaseUrl();
        internalServiceAuthSupport.authorize(restClient.post()
                .uri(baseUrl + "/internal/v1/orders/{orderId}/transition", orderId)
                .body(new OrderTransitionRequest(newStatus, changedBy, reason)))
                .retrieve()
                .toBodilessEntity();
        log.debug("Requested order {} transition to {} via {}", orderId, newStatus, baseUrl);
    }
}
