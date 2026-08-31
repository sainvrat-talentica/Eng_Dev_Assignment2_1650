package com.swifteats.tracking.config;

import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@EnableConfigurationProperties(TrackingProperties.class)
@ServiceScope(ServiceName.BACKEND)
public class TrackingConfig {

    public static final String GPS_LOCATIONS_TOPIC = "gps.locations";

    @Bean
    @ConditionalOnProperty(name = "swifteats.tracking.kafka-enabled", havingValue = "true", matchIfMissing = true)
    NewTopic gpsLocationsTopic() {
        return TopicBuilder.name(GPS_LOCATIONS_TOPIC)
                .partitions(6)
                .replicas(1)
                .build();
    }
}
