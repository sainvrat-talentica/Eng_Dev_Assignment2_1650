package com.swifteats.order.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentProcessMessage(
        UUID orderId,
        BigDecimal amount) {
}
