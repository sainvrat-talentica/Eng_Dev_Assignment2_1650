package com.swifteats.gateway;

import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;
import com.swifteats.gateway.config.ServiceRegistryProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
@ConditionalOnProperty(name = "swifteats.gateway.enabled", havingValue = "true")
@ServiceScope(ServiceName.BACKEND)
public class OpenApiProxyController {

    private final RestClient restClient;
    private final ServiceRegistryProperties registry;

    public OpenApiProxyController(RestClient.Builder restClientBuilder, ServiceRegistryProperties registry) {
        this.restClient = restClientBuilder.build();
        this.registry = registry;
    }

    @GetMapping("/v3/api-docs/{service}")
    public ResponseEntity<String> proxyOpenApi(@PathVariable String service) {
        String baseUrl = switch (service.toLowerCase()) {
            case "entities" -> registry.getEntities().getBaseUrl();
            case "order" -> registry.getOrder().getBaseUrl();
            case "payment" -> registry.getPayment().getBaseUrl();
            case "refund" -> registry.getRefund().getBaseUrl();
            case "analytics" -> registry.getAnalytics().getBaseUrl();
            case "backend" -> registry.getBackend().getBaseUrl();
            default -> null;
        };
        if (baseUrl == null) {
            return ResponseEntity.notFound().build();
        }
        String body = restClient.get()
                .uri(baseUrl + "/v3/api-docs")
                .retrieve()
                .body(String.class);
        return ResponseEntity.ok(body);
    }
}
