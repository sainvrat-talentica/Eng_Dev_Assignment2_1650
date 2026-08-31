package com.swifteats.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "swifteats.payment")
public class PaymentProperties {

    private boolean messagingEnabled = true;
    private double mockFailureRate = 0.05;
    private long mockDelayMs = 200;

    public boolean isMessagingEnabled() {
        return messagingEnabled;
    }

    public void setMessagingEnabled(boolean messagingEnabled) {
        this.messagingEnabled = messagingEnabled;
    }

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
