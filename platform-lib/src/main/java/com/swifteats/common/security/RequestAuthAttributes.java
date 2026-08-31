package com.swifteats.common.security;

import com.swifteats.common.exception.AuthenticationRequiredException;

import java.util.UUID;

public final class RequestAuthAttributes {

    public static final String CUSTOMER_ID = "swifteats.auth.customerId";

    private RequestAuthAttributes() {
    }

    public static UUID customerId(jakarta.servlet.http.HttpServletRequest request) {
        Object value = request.getAttribute(CUSTOMER_ID);
        if (value instanceof UUID uuid) {
            return uuid;
        }
        throw new AuthenticationRequiredException("Authenticated customer context is required");
    }
}
