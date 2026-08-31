package com.swifteats.common.security;

import com.swifteats.common.domain.CustomerStatus;
import com.swifteats.order.entity.Customer;
import com.swifteats.order.repository.CustomerRepository;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerAuthFilterTest {

    private static final UUID CUSTOMER_ID = UUID.fromString("44444444-4444-4444-4444-444444444401");
    private static final String TOKEN = "demo-customer-token-local-only";

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private FilterChain filterChain;

    private CustomerAuthFilter filter;

    @BeforeEach
    void setUp() {
        filter = new CustomerAuthFilter(customerRepository);
    }

    @Test
    void skipsUnprotectedPaths() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/restaurants");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void rejectsMissingHeadersOnOrderCreate() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/orders");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void rejectsSpoofedCustomerIdWithoutValidToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/orders");
        request.addHeader("X-Customer-Id", CUSTOMER_ID.toString());
        request.addHeader("X-Customer-Api-Key", "wrong-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(customerRepository.findByIdAndApiToken(CUSTOMER_ID, "wrong-token")).thenReturn(Optional.empty());

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void allowsValidCredentialsAndSetsCustomerAttribute() throws Exception {
        UUID orderId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/orders/" + orderId);
        request.addHeader("X-Customer-Id", CUSTOMER_ID.toString());
        request.addHeader("X-Customer-Api-Key", TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(customerRepository.findByIdAndApiToken(CUSTOMER_ID, TOKEN)).thenReturn(Optional.of(activeCustomer()));

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(request.getAttribute(RequestAuthAttributes.CUSTOMER_ID)).isEqualTo(CUSTOMER_ID);
    }

    @Test
    void rejectsCustomerIdOnlyWithoutTokenOnTrackingPath() throws Exception {
        UUID orderId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/orders/" + orderId + "/tracking");
        request.addHeader("X-Customer-Id", CUSTOMER_ID.toString());
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(customerRepository, never()).findByIdAndApiToken(any(), any());
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void allowsValidCredentialsOnRefundCreate() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/refunds");
        request.addHeader("X-Customer-Id", CUSTOMER_ID.toString());
        request.addHeader("X-Customer-Api-Key", TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(customerRepository.findByIdAndApiToken(CUSTOMER_ID, TOKEN)).thenReturn(Optional.of(activeCustomer()));

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(request.getAttribute(RequestAuthAttributes.CUSTOMER_ID)).isEqualTo(CUSTOMER_ID);
    }

    private static Customer activeCustomer() {
        Customer customer = new Customer();
        customer.setId(CUSTOMER_ID);
        customer.setStatus(CustomerStatus.ACTIVE);
        return customer;
    }
}
