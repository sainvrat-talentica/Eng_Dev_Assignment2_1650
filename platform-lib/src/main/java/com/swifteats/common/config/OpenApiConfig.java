package com.swifteats.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String ADMIN_API_KEY = "AdminApiKey";
    public static final String CUSTOMER_ID = "CustomerId";
    public static final String CUSTOMER_API_KEY = "CustomerApiKey";
    public static final String DRIVER_API_KEY = "DriverApiKey";

    @Bean
    public OpenAPI swifteatsOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SwiftEats API")
                        .version("1.0.0")
                        .description("""
                                Real-time food delivery platform — restaurants, orders, GPS tracking, and analytics.

                                **Order flow:** `PENDING_PAYMENT` → `CONFIRMED` → `PREPARING` → `OUT_FOR_DELIVERY` → `DELIVERED`

                                Use **Authorize** to set API keys before trying protected endpoints.
                                """)
                        .license(new License().name("MIT")))
                .components(new Components()
                        .addSecuritySchemes(ADMIN_API_KEY, apiKeyHeader("X-Admin-Api-Key"))
                        .addSecuritySchemes(CUSTOMER_ID, apiKeyHeader("X-Customer-Id"))
                        .addSecuritySchemes(CUSTOMER_API_KEY, apiKeyHeader("X-Customer-Api-Key"))
                        .addSecuritySchemes(DRIVER_API_KEY, apiKeyHeader("X-Driver-Api-Key")));
    }

    private static SecurityScheme apiKeyHeader(String headerName) {
        return new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name(headerName);
    }
}
