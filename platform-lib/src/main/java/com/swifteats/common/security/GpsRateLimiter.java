package com.swifteats.common.security;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class GpsRateLimiter {

    private final SecurityProperties securityProperties;
    private final Map<UUID, Window> windows = new ConcurrentHashMap<>();

    public GpsRateLimiter(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    public boolean tryAcquire(UUID driverId) {
        int limit = Math.max(1, securityProperties.getGpsRateLimitPerSecond());
        long second = System.currentTimeMillis() / 1000L;
        Window window = windows.computeIfAbsent(driverId, ignored -> new Window(second));
        synchronized (window) {
            if (window.second != second) {
                window.second = second;
                window.count.set(0);
            }
            return window.count.incrementAndGet() <= limit;
        }
    }

    private static final class Window {
        private volatile long second;
        private final AtomicInteger count = new AtomicInteger();

        private Window(long second) {
            this.second = second;
        }
    }
}
