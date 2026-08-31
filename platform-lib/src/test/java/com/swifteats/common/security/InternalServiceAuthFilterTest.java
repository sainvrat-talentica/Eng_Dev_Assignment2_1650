package com.swifteats.common.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class InternalServiceAuthFilterTest {

    private static final String INTERNAL_KEY = "test-internal-key";

    private InternalServiceAuthFilter filter;
    private boolean chainInvoked;

    @BeforeEach
    void setUp() {
        filter = new InternalServiceAuthFilter(INTERNAL_KEY);
        chainInvoked = false;
    }

    @Test
    void allowsInternalRequestWithValidKey() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/internal/v1/orders/11111111-1111-1111-1111-111111111111/transition");
        request.addHeader(InternalServiceAuthFilter.INTERNAL_SERVICE_KEY_HEADER, INTERNAL_KEY);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain());

        assertThat(chainInvoked).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void rejectsInternalRequestWithoutKey() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/internal/v1/payments/process");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain());

        assertThat(chainInvoked).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void skipsPublicApiPaths() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/orders");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain());

        assertThat(chainInvoked).isTrue();
    }

    private FilterChain chain() {
        return (request, response) -> chainInvoked = true;
    }
}
