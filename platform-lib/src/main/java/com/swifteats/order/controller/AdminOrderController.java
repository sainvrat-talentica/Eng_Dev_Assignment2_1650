package com.swifteats.order.controller;

import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;

import com.swifteats.common.config.OpenApiConfig;
import com.swifteats.order.dto.OrderResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import com.swifteats.order.dto.PaymentRequest;
import com.swifteats.order.dto.PaymentResult;
import com.swifteats.order.dto.TransitionOrderRequest;
import com.swifteats.order.payment.MockPaymentGateway;
import com.swifteats.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/orders")
@SecurityRequirement(name = OpenApiConfig.ADMIN_API_KEY)
@ServiceScope(ServiceName.ORDER)
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PatchMapping("/{orderId}/state")
    public OrderResponse transitionOrder(
            @PathVariable UUID orderId,
            @Valid @RequestBody TransitionOrderRequest request) {
        String changedBy = request.changedBy() != null ? request.changedBy() : "ADMIN";
        return orderService.transition(orderId, request.status(), changedBy, request.reason());
    }
}
