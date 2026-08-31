package com.swifteats.common.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class SecurityStartupValidator {

    private static final Logger log = LoggerFactory.getLogger(SecurityStartupValidator.class);
    private static final Set<String> INSECURE_ADMIN_KEYS = Set.of(
            "dev-admin-key", "change-me", "admin", "local-dev-admin-key-change-me");
    private static final Set<String> INSECURE_INTERNAL_KEYS = Set.of(
            "dev-internal-key", "change-me", "local-dev-internal-key-change-me");

    private final String adminApiKey;
    private final String internalApiKey;
    private final SecurityProperties securityProperties;

    public SecurityStartupValidator(
            @Value("${swifteats.admin.api-key}") String adminApiKey,
            @Value("${swifteats.internal.api-key}") String internalApiKey,
            SecurityProperties securityProperties) {
        this.adminApiKey = adminApiKey;
        this.internalApiKey = internalApiKey;
        this.securityProperties = securityProperties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validateSecurityConfiguration() {
        if (!securityProperties.isAuthEnabled()) {
            throw new IllegalStateException(
                    "SWIFTEATS_AUTH_ENABLED=false is not supported. Customer and driver auth are always enforced.");
        }
        if (INSECURE_ADMIN_KEYS.contains(adminApiKey) && !securityProperties.isAllowInsecureDefaults()) {
            throw new IllegalStateException(
                    "Insecure ADMIN_API_KEY detected. Set a strong key or enable "
                            + "SWIFTEATS_ALLOW_INSECURE_DEFAULTS=true for local development only.");
        }
        if (INSECURE_INTERNAL_KEYS.contains(internalApiKey) && !securityProperties.isAllowInsecureDefaults()) {
            throw new IllegalStateException(
                    "Insecure INTERNAL_SERVICE_API_KEY detected. Set a strong key or enable "
                            + "SWIFTEATS_ALLOW_INSECURE_DEFAULTS=true for local development only.");
        }
        if (securityProperties.isAllowInsecureDefaults()) {
            log.warn("SWIFTEATS_ALLOW_INSECURE_DEFAULTS=true — do not expose this stack to untrusted networks");
        }
    }
}
