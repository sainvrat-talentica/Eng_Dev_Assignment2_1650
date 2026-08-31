package com.swifteats.gateway;

import com.swifteats.gateway.config.ServiceRegistryProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GatewayProxyFilterTest {

    private ServiceRegistryProperties registry;
    private TestableGatewayProxyFilter filter;

    @BeforeEach
    void setUp() {
        registry = new ServiceRegistryProperties();
        registry.getEntities().setBaseUrl("http://entities:8081");
        registry.getOrder().setBaseUrl("http://order:8082");
        registry.getPayment().setBaseUrl("http://payment:8083");
        registry.getRefund().setBaseUrl("http://refund:8084");
        registry.getAnalytics().setBaseUrl("http://analytics:8085");
        filter = new TestableGatewayProxyFilter(RestClient.builder(), registry);
    }

    @ParameterizedTest
    @MethodSource("pathsThatShouldNotBeFiltered")
    void shouldNotFilter_skipsNonProxyPaths(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);

        assertThat(filter.exposeShouldNotFilter(request)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("pathsThatShouldBeFiltered")
    void shouldNotFilter_proxiesEligiblePaths(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);

        assertThat(filter.exposeShouldNotFilter(request)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("resolveTargetCases")
    void resolveTarget_mapsPathToServiceBase(String path, String expectedBase) {
        assertThat(filter.exposeResolveTarget(path)).isEqualTo(expectedBase);
    }

    @Test
    void resolveTarget_returnsNullForUnmappedApiPath() {
        assertThat(filter.exposeResolveTarget("/api/v1/unknown/resource")).isNull();
    }

    @Test
    void doFilterInternal_proxiesEligibleRequest() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://order:8082/api/v1/orders?page=1"))
                .andRespond(withSuccess("{\"ok\":true}", MediaType.APPLICATION_JSON));

        GatewayProxyFilter proxy = new GatewayProxyFilter(builder, registry);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/orders");
        request.setQueryString("page=1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        proxy.doFilterInternal(request, response, (req, res) -> {
            throw new AssertionError("Should proxy instead of delegating");
        });

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString()).contains("ok");
        server.verify();
    }

    @Test
    void doFilterInternal_delegatesWhenTargetUnknown() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        GatewayProxyFilter proxy = new GatewayProxyFilter(builder, registry);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/unknown");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean delegated = new AtomicBoolean(false);

        proxy.doFilterInternal(request, response, (req, res) -> delegated.set(true));

        assertThat(delegated).isTrue();
    }

    @Test
    void doFilterInternal_forwardsRequestBodyOnPost() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://refund:8084/api/v1/refunds"))
                .andRespond(withSuccess("accepted", MediaType.TEXT_PLAIN));

        GatewayProxyFilter proxy = new GatewayProxyFilter(builder, registry);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/refunds");
        request.setContent("{\"orderId\":\"abc\"}".getBytes());
        request.addHeader("X-Custom-Header", "value");
        MockHttpServletResponse response = new MockHttpServletResponse();

        proxy.doFilterInternal(request, response, (req, res) -> {
            throw new AssertionError("Should proxy");
        });

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString()).isEqualTo("accepted");
        server.verify();
    }

    private static Stream<String> pathsThatShouldNotBeFiltered() {
        return Stream.of(
                "/health",
                "/api/v1/auth/login",
                "/api/v1/admin/customers",
                "/api/v1/restaurants",
                "/api/v1/drivers/locations",
                "/api/v1/orders/abc/tracking");
    }

    private static Stream<String> pathsThatShouldBeFiltered() {
        return Stream.of(
                "/api/v1/admin/restaurants",
                "/api/v1/admin/orders",
                "/api/v1/admin/analytics/import",
                "/api/v1/orders",
                "/api/v1/payments/capture",
                "/api/v1/refunds",
                "/api/v1/analytics/insights");
    }

    private static Stream<Arguments> resolveTargetCases() {
        return Stream.of(
                Arguments.of("/api/v1/admin/restaurants/1", "http://entities:8081"),
                Arguments.of("/api/v1/admin/orders", "http://order:8082"),
                Arguments.of("/api/v1/orders/123", "http://order:8082"),
                Arguments.of("/api/v1/payments/refund", "http://payment:8083"),
                Arguments.of("/api/v1/refunds/456", "http://refund:8084"),
                Arguments.of("/api/v1/analytics/query", "http://analytics:8085"),
                Arguments.of("/api/v1/admin/analytics/import", "http://analytics:8085"));
    }

    private static final class TestableGatewayProxyFilter extends GatewayProxyFilter {

        TestableGatewayProxyFilter(RestClient.Builder restClientBuilder, ServiceRegistryProperties registry) {
            super(restClientBuilder, registry);
        }

        boolean exposeShouldNotFilter(HttpServletRequest request) {
            return shouldNotFilter(request);
        }

        String exposeResolveTarget(String path) {
            return ReflectionTestUtils.invokeMethod(this, "resolveTarget", path);
        }
    }
}
