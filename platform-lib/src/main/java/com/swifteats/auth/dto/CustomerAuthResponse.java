package com.swifteats.auth.dto;

public record CustomerAuthResponse(
        CustomerProfileResponse profile,
        String customerId,
        String apiToken) {}
