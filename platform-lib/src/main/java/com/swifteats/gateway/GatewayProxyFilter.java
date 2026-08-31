package com.swifteats.gateway;

import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;
import com.swifteats.gateway.config.ServiceRegistryProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Set;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@ConditionalOnProperty(name = "swifteats.gateway.enabled", havingValue = "true")
@ServiceScope(ServiceName.BACKEND)
public class GatewayProxyFilter extends OncePerRequestFilter {

    private static final Set<String> HOP_BY_HOP = Set.of(
            "connection", "keep-alive", "proxy-authenticate", "proxy-authorization",
            "te", "trailers", "transfer-encoding", "upgrade", "host", "content-length");

    private final RestClient restClient;
    private final ServiceRegistryProperties registry;

    public GatewayProxyFilter(RestClient.Builder restClientBuilder, ServiceRegistryProperties registry) {
        this.restClient = restClientBuilder.build();
        this.registry = registry;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (!path.startsWith("/api/v1/")) {
            return true;
        }
        if (path.startsWith("/api/v1/auth")
                || path.startsWith("/api/v1/admin/customers")
                || path.startsWith("/api/v1/restaurants")
                || path.startsWith("/api/v1/drivers")
                || path.contains("/tracking")) {
            return true;
        }
        return !(path.startsWith("/api/v1/admin/restaurants")
                || path.startsWith("/api/v1/admin/orders")
                || path.startsWith("/api/v1/admin/analytics")
                || path.startsWith("/api/v1/orders")
                || path.startsWith("/api/v1/payments")
                || path.startsWith("/api/v1/refunds")
                || path.startsWith("/api/v1/analytics"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        String targetBase = resolveTarget(path);
        if (targetBase == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String query = request.getQueryString();
        String targetUrl = targetBase + path + (query != null ? "?" + query : "");
        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        byte[] body = StreamUtils.copyToByteArray(request.getInputStream());

        RestClient.RequestBodySpec spec = restClient.method(method).uri(targetUrl);
        copyHeaders(request, spec);
        ResponseEntity<byte[]> upstream = (body.length == 0 ? spec : spec.body(body))
                .retrieve()
                .toEntity(byte[].class);

        response.setStatus(upstream.getStatusCode().value());
        upstream.getHeaders().forEach((name, values) -> {
            if (!HOP_BY_HOP.contains(name.toLowerCase())) {
                for (String value : values) {
                    response.addHeader(name, value);
                }
            }
        });
        if (upstream.getBody() != null) {
            response.getOutputStream().write(upstream.getBody());
        }
    }

    private String resolveTarget(String path) {
        if (path.startsWith("/api/v1/admin/restaurants")) {
            return registry.getEntities().getBaseUrl();
        }
        if (path.startsWith("/api/v1/admin/orders") || path.startsWith("/api/v1/orders")) {
            return registry.getOrder().getBaseUrl();
        }
        if (path.startsWith("/api/v1/payments")) {
            return registry.getPayment().getBaseUrl();
        }
        if (path.startsWith("/api/v1/refunds")) {
            return registry.getRefund().getBaseUrl();
        }
        if (path.startsWith("/api/v1/analytics") || path.startsWith("/api/v1/admin/analytics")) {
            return registry.getAnalytics().getBaseUrl();
        }
        return null;
    }

    private void copyHeaders(HttpServletRequest request, RestClient.RequestBodySpec spec) {
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            if (HOP_BY_HOP.contains(name.toLowerCase())) {
                continue;
            }
            Enumeration<String> values = request.getHeaders(name);
            while (values.hasMoreElements()) {
                spec.header(name, values.nextElement());
            }
        }
    }
}
