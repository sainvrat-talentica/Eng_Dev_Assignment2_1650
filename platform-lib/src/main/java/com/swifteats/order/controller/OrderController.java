package com.swifteats.order.controller;

import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;

import com.swifteats.common.config.OpenApiConfig;
import com.swifteats.common.security.RequestAuthAttributes;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import com.swifteats.order.dto.CreateOrderRequest;
import com.swifteats.order.dto.OrderAcceptedResponse;
import com.swifteats.order.dto.OrderResponse;
import com.swifteats.order.dto.OrderStateHistoryResponse;
import com.swifteats.order.dto.OrderSummaryResponse;
import com.swifteats.order.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@SecurityRequirements({
        @SecurityRequirement(name = OpenApiConfig.CUSTOMER_ID),
        @SecurityRequirement(name = OpenApiConfig.CUSTOMER_API_KEY)
})
@ServiceScope(ServiceName.ORDER)
public class OrderController {

    public static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public OrderAcceptedResponse createOrder(
            HttpServletRequest httpRequest,
            @RequestHeader(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Valid @RequestBody CreateOrderRequest request) {
        UUID customerId = RequestAuthAttributes.customerId(httpRequest);
        return orderService.createOrder(customerId, idempotencyKey, request);
    }

    @GetMapping
    public List<OrderSummaryResponse> listOrders(
            HttpServletRequest httpRequest,
            @RequestParam(defaultValue = "all") String scope) {
        UUID customerId = RequestAuthAttributes.customerId(httpRequest);
        return orderService.listCustomerOrders(customerId, scope);
    }

    @GetMapping("/{orderId}")
    public OrderResponse getOrder(HttpServletRequest httpRequest, @PathVariable UUID orderId) {
        UUID customerId = RequestAuthAttributes.customerId(httpRequest);
        return orderService.getOrder(orderId, customerId);
    }

    @PostMapping("/{orderId}/pay")
    public OrderResponse payOrder(HttpServletRequest httpRequest, @PathVariable UUID orderId) {
        UUID customerId = RequestAuthAttributes.customerId(httpRequest);
        return orderService.initiatePayment(orderId, customerId);
    }

    @GetMapping("/{orderId}/history")
    public List<OrderStateHistoryResponse> getOrderHistory(HttpServletRequest httpRequest, @PathVariable UUID orderId) {
        UUID customerId = RequestAuthAttributes.customerId(httpRequest);
        return orderService.getOrderHistory(orderId, customerId);
    }
}
