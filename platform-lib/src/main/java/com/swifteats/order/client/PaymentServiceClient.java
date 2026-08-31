package com.swifteats.order.client;

import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;
import com.swifteats.common.security.InternalServiceAuthSupport;
import com.swifteats.gateway.config.ServiceRegistryProperties;
import com.swifteats.order.dto.PaymentProcessMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ServiceScope(ServiceName.ORDER)
public class PaymentServiceClient {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceClient.class);

    private final RestClient restClient;
    private final ServiceRegistryProperties serviceRegistry;
    private final InternalServiceAuthSupport internalServiceAuthSupport;

    public PaymentServiceClient(
            RestClient.Builder restClientBuilder,
            ServiceRegistryProperties serviceRegistry,
            InternalServiceAuthSupport internalServiceAuthSupport) {
        this.restClient = restClientBuilder.build();
        this.serviceRegistry = serviceRegistry;
        this.internalServiceAuthSupport = internalServiceAuthSupport;
    }

    public void processPayment(PaymentProcessMessage message) {
        String baseUrl = serviceRegistry.getPayment().getBaseUrl();
        internalServiceAuthSupport.authorize(restClient.post()
                .uri(baseUrl + "/internal/v1/payments/process")
                .body(message))
                .retrieve()
                .toBodilessEntity();
        log.debug("Requested synchronous payment for order {} via {}", message.orderId(), baseUrl);
    }
}
