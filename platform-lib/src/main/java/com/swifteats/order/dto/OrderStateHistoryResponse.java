package com.swifteats.order.dto;

import com.swifteats.common.domain.OrderStatus;

import java.time.Instant;
import java.util.UUID;

public record OrderStateHistoryResponse(
        UUID id,
        String fromStatus,
        OrderStatus toStatus,
        String changedBy,
        String reason,
        Instant changedAt) {
}
