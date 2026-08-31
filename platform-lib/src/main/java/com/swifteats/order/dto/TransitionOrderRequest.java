package com.swifteats.order.dto;

import com.swifteats.common.domain.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record TransitionOrderRequest(
        @NotNull OrderStatus status,
        String reason,
        String changedBy) {
}
