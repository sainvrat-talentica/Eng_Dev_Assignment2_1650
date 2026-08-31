package com.swifteats.auth.dto;

import java.time.Instant;
import java.util.UUID;

public record CustomerProfileResponse(
        UUID customerId,
        String name,
        String email,
        String phone,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String pincode,
        Instant createdAt,
        Instant updatedAt) {}
