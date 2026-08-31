package com.swifteats.common.security;

import com.swifteats.tracking.repository.DriverRepository;
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
@Order(21)
public class DriverAuthFilter extends OncePerRequestFilter {

    private static final String DRIVER_TOKEN_HEADER = "X-Driver-Api-Key";
    private static final Pattern DRIVER_LOCATION_PATH =
            Pattern.compile("^/api/v1/drivers/([0-9a-fA-F-]{36})/location/?$");

    private final DriverRepository driverRepository;
    private final GpsRateLimiter gpsRateLimiter;

    public DriverAuthFilter(DriverRepository driverRepository, GpsRateLimiter gpsRateLimiter) {
        this.driverRepository = driverRepository;
        this.gpsRateLimiter = gpsRateLimiter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod()) || !DRIVER_LOCATION_PATH.matcher(request.getRequestURI()).matches();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Matcher matcher = DRIVER_LOCATION_PATH.matcher(request.getRequestURI());
        if (!matcher.matches()) {
            filterChain.doFilter(request, response);
            return;
        }

        UUID driverId;
        try {
            driverId = UUID.fromString(matcher.group(1));
        } catch (IllegalArgumentException ex) {
            unauthorized(response, "Invalid driver id in path");
            return;
        }

        String token = request.getHeader(DRIVER_TOKEN_HEADER);
        if (token == null || token.isBlank()) {
            unauthorized(response, "Missing X-Driver-Api-Key");
            return;
        }

        if (!driverRepository.existsByIdAndApiToken(driverId, token)) {
            unauthorized(response, "Invalid driver credentials");
            return;
        }

        if (!gpsRateLimiter.tryAcquire(driverId)) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("""
                    {"code":"RATE_LIMITED","message":"GPS update rate limit exceeded"}
                    """);
            return;
        }

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
