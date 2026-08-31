package com.swifteats.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class InternalServiceAuthSupport {

    private final String internalApiKey;

    public InternalServiceAuthSupport(@Value("${swifteats.internal.api-key}") String internalApiKey) {
        this.internalApiKey = internalApiKey;
    }

    public RestClient.RequestHeadersSpec<?> authorize(RestClient.RequestHeadersSpec<?> spec) {
        return spec.header(InternalServiceAuthFilter.INTERNAL_SERVICE_KEY_HEADER, internalApiKey);
    }
}
