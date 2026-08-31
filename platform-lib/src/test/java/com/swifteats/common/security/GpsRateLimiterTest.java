package com.swifteats.common.security;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GpsRateLimiterTest {

    @Test
    void tryAcquire_allowsUpToLimitPerSecond() {
        SecurityProperties properties = new SecurityProperties();
        properties.setGpsRateLimitPerSecond(2);
        GpsRateLimiter limiter = new GpsRateLimiter(properties);
        UUID driverId = UUID.randomUUID();

        assertThat(limiter.tryAcquire(driverId)).isTrue();
        assertThat(limiter.tryAcquire(driverId)).isTrue();
        assertThat(limiter.tryAcquire(driverId)).isFalse();
    }

    @Test
    void tryAcquire_usesMinimumLimitOfOneWhenConfiguredZero() {
        SecurityProperties properties = new SecurityProperties();
        properties.setGpsRateLimitPerSecond(0);
        GpsRateLimiter limiter = new GpsRateLimiter(properties);
        UUID driverId = UUID.randomUUID();

        assertThat(limiter.tryAcquire(driverId)).isTrue();
        assertThat(limiter.tryAcquire(driverId)).isFalse();
    }

    @Test
    void tryAcquire_tracksDriversIndependently() {
        SecurityProperties properties = new SecurityProperties();
        properties.setGpsRateLimitPerSecond(1);
        GpsRateLimiter limiter = new GpsRateLimiter(properties);
        UUID firstDriver = UUID.randomUUID();
        UUID secondDriver = UUID.randomUUID();

        assertThat(limiter.tryAcquire(firstDriver)).isTrue();
        assertThat(limiter.tryAcquire(secondDriver)).isTrue();
        assertThat(limiter.tryAcquire(firstDriver)).isFalse();
        assertThat(limiter.tryAcquire(secondDriver)).isFalse();
    }

    @Test
    void tryAcquire_resetsCountAfterSecondBoundary() throws InterruptedException {
        SecurityProperties properties = new SecurityProperties();
        properties.setGpsRateLimitPerSecond(1);
        GpsRateLimiter limiter = new GpsRateLimiter(properties);
        UUID driverId = UUID.randomUUID();

        assertThat(limiter.tryAcquire(driverId)).isTrue();
        assertThat(limiter.tryAcquire(driverId)).isFalse();

        Thread.sleep(1_100L);

        assertThat(limiter.tryAcquire(driverId)).isTrue();
    }
}
