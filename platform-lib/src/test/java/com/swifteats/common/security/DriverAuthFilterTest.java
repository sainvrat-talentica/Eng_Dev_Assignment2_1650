package com.swifteats.common.security;

import com.swifteats.tracking.repository.DriverRepository;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverAuthFilterTest {

    private static final UUID DRIVER_ID = UUID.fromString("55555555-5555-5555-5555-555555555501");
    private static final String TOKEN = "demo-driver-token-001";

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private GpsRateLimiter gpsRateLimiter;

    @Mock
    private FilterChain filterChain;

    private DriverAuthFilter filter;

    @BeforeEach
    void setUp() {
        filter = new DriverAuthFilter(driverRepository, gpsRateLimiter);
    }

    @Test
    void rejectsMissingDriverToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/api/v1/drivers/" + DRIVER_ID + "/location");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void rejectsInvalidDriverToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/api/v1/drivers/" + DRIVER_ID + "/location");
        request.addHeader("X-Driver-Api-Key", "invalid");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(driverRepository.existsByIdAndApiToken(DRIVER_ID, "invalid")).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(gpsRateLimiter, never()).tryAcquire(any());
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void allowsValidDriverToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/api/v1/drivers/" + DRIVER_ID + "/location");
        request.addHeader("X-Driver-Api-Key", TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(driverRepository.existsByIdAndApiToken(DRIVER_ID, TOKEN)).thenReturn(true);
        when(gpsRateLimiter.tryAcquire(DRIVER_ID)).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void returns429WhenRateLimited() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/api/v1/drivers/" + DRIVER_ID + "/location");
        request.addHeader("X-Driver-Api-Key", TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(driverRepository.existsByIdAndApiToken(DRIVER_ID, TOKEN)).thenReturn(true);
        when(gpsRateLimiter.tryAcquire(DRIVER_ID)).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(429);
        verify(filterChain, never()).doFilter(any(), any());
    }
}
