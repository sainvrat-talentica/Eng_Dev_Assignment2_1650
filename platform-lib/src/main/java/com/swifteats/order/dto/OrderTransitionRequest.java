package com.swifteats.order.dto;

import com.swifteats.common.domain.OrderStatus;

public record OrderTransitionRequest(OrderStatus status, String changedBy, String reason) {
}
