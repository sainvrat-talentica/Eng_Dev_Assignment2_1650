package com.swifteats.refund.dto;

import java.util.UUID;

public record RefundAcceptedResponse(UUID refundId, String status) {
}
