package com.swifteats.refund.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "swifteats.refund")
public class RefundProperties {

    private double mockFailureRate = 0.05;
    private long mockDelayMs = 300;

    public double getMockFailureRate() {
        return mockFailureRate;
    }

    public void setMockFailureRate(double mockFailureRate) {
        this.mockFailureRate = mockFailureRate;
    }

    public long getMockDelayMs() {
        return mockDelayMs;
    }

    public void setMockDelayMs(long mockDelayMs) {
        this.mockDelayMs = mockDelayMs;
    }
}
