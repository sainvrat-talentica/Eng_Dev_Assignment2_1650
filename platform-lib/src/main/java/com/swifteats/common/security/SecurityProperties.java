package com.swifteats.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "swifteats.security")
public class SecurityProperties {

    /** Must remain true — disabling auth is rejected at startup. */
    private boolean authEnabled = true;

    /** Allow documented default credentials; must be false outside local dev. */
    private boolean allowInsecureDefaults = false;

    /** Max GPS POSTs per driver per second. */
    private int gpsRateLimitPerSecond = 10;

    public boolean isAuthEnabled() {
        return authEnabled;
    }

    public void setAuthEnabled(boolean authEnabled) {
        this.authEnabled = authEnabled;
    }

    public boolean isAllowInsecureDefaults() {
        return allowInsecureDefaults;
    }

    public void setAllowInsecureDefaults(boolean allowInsecureDefaults) {
        this.allowInsecureDefaults = allowInsecureDefaults;
    }

    public int getGpsRateLimitPerSecond() {
        return gpsRateLimitPerSecond;
    }

    public void setGpsRateLimitPerSecond(int gpsRateLimitPerSecond) {
        this.gpsRateLimitPerSecond = gpsRateLimitPerSecond;
    }
}
