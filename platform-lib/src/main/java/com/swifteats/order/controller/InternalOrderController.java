package com.swifteats.order.controller;

import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;
import com.swifteats.order.dto.OrderResponse;
import com.swifteats.order.dto.OrderTransitionRequest;
import com.swifteats.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/v1/orders")
@ServiceScope(ServiceName.ORDER)
public class InternalOrderController {

    private final OrderService orderService;

    public InternalOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/{orderId}/transition")
    public OrderResponse transition(
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderTransitionRequest request) {
        return orderService.transition(orderId, request.status(), request.changedBy(), request.reason());
    }
}
