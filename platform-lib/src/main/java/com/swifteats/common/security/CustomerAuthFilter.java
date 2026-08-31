package com.swifteats.common.security;

import com.swifteats.common.domain.CustomerStatus;
import com.swifteats.order.repository.CustomerRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Order(20)
public class CustomerAuthFilter extends OncePerRequestFilter {

    private static final String CUSTOMER_ID_HEADER = "X-Customer-Id";
    private static final String CUSTOMER_TOKEN_HEADER = "X-Customer-Api-Key";
    private static final Pattern ORDER_PATH =
            Pattern.compile("^/api/v1/orders(?:/([0-9a-fA-F-]{36})(?:/(history|tracking(?:/stream)?|pay)?)?)?/?$");
    private static final Pattern AUTH_PROFILE_PATH = Pattern.compile("^/api/v1/auth/(me|profile)/?$");
    private static final Pattern REFUND_PATH =
            Pattern.compile("^/api/v1/refunds(?:/([0-9a-fA-F-]{36})?)?/?$");

    private final CustomerRepository customerRepository;

    public CustomerAuthFilter(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !isProtectedCustomerPath(request);
    }

    private boolean isProtectedCustomerPath(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        if ("POST".equals(method) && "/api/v1/orders".equals(path)) {
            return true;
        }
        if ("GET".equals(method) && "/api/v1/orders".equals(path)) {
            return true;
        }
        if ("GET".equals(method) && AUTH_PROFILE_PATH.matcher(path).matches()) {
            return true;
        }
        if ("PATCH".equals(method) && "/api/v1/auth/profile".equals(path)) {
            return true;
        }
        if (("POST".equals(method) || "GET".equals(method)) && REFUND_PATH.matcher(path).matches()) {
            return true;
        }
        Matcher matcher = ORDER_PATH.matcher(path);
        if (!matcher.matches() || matcher.group(1) == null) {
            return false;
        }
        if ("POST".equals(method) && "pay".equals(matcher.group(2))) {
            return true;
        }
        return "GET".equals(method);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String customerIdHeader = request.getHeader(CUSTOMER_ID_HEADER);
        String token = request.getHeader(CUSTOMER_TOKEN_HEADER);
        if (customerIdHeader == null || token == null || token.isBlank()) {
            unauthorized(response, "Missing X-Customer-Id or X-Customer-Api-Key");
            return;
        }

        UUID customerId;
        try {
            customerId = UUID.fromString(customerIdHeader.trim());
        } catch (IllegalArgumentException ex) {
            unauthorized(response, "Invalid X-Customer-Id");
            return;
        }

        var customer = customerRepository.findByIdAndApiToken(customerId, token);
        if (customer.isEmpty()) {
            unauthorized(response, "Invalid customer credentials");
            return;
        }
        if (customer.get().getStatus() != CustomerStatus.ACTIVE) {
            unauthorized(response, "Account is deactivated");
            return;
        }

        request.setAttribute(RequestAuthAttributes.CUSTOMER_ID, customerId);
        filterChain.doFilter(request, response);
    }

    private void unauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("""
                {"code":"UNAUTHORIZED","message":"%s"}
                """.formatted(message));
    }
}
