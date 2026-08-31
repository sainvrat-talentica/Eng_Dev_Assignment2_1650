package com.swifteats.refund.dto;

import com.swifteats.common.domain.RefundStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RefundResponse(
        UUID id,
        UUID orderId,
        UUID customerId,
        BigDecimal amount,
        RefundStatus status,
        String reason,
        String failureReason,
        Instant createdAt,
        Instant updatedAt,
        Instant completedAt) {
}
