package com.swifteats.common.domain;

/**
 * Marks Flyway seed and internal demo records with a shared name prefix.
 * Runtime user registration and admin onboarding store names as entered.
 */
public final class TemporaryDataLabels {

    public static final String PREFIX = "[T] ";

    private TemporaryDataLabels() {
    }

    public static String prefix(String name) {
        if (name == null) {
            return null;
        }
        String trimmed = name.trim();
        if (trimmed.isEmpty() || trimmed.startsWith(PREFIX)) {
            return trimmed;
        }
        return PREFIX + trimmed;
    }
}
