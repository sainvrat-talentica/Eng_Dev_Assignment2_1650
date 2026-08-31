package com.swifteats.apps.analytics;

import com.swifteats.common.runtime.SwiftEatsServiceApplication;
import org.springframework.boot.SpringApplication;

@SwiftEatsServiceApplication
public class AnalyticsServiceApplication {

    public static void main(String[] args) {
        System.setProperty("swifteats.service.name", "ANALYTICS");
        SpringApplication.run(AnalyticsServiceApplication.class, args);
    }
}
