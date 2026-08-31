package com.swifteats.auth;

import java.security.SecureRandom;
import java.util.Base64;

public final class ApiTokenGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private ApiTokenGenerator() {
    }

    public static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
