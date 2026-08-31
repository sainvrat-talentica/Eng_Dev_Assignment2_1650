package com.swifteats.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "swifteats.services")
public class ServiceRegistryProperties {

    private ServiceEndpoint entities = new ServiceEndpoint("http://localhost:8081");
    private ServiceEndpoint order = new ServiceEndpoint("http://localhost:8082");
    private ServiceEndpoint payment = new ServiceEndpoint("http://localhost:8083");
    private ServiceEndpoint refund = new ServiceEndpoint("http://localhost:8084");
    private ServiceEndpoint analytics = new ServiceEndpoint("http://localhost:8085");
    private ServiceEndpoint backend = new ServiceEndpoint("http://localhost:8080");

    public ServiceEndpoint getEntities() {
        return entities;
    }

    public void setEntities(ServiceEndpoint entities) {
        this.entities = entities;
    }

    public ServiceEndpoint getOrder() {
        return order;
    }

    public void setOrder(ServiceEndpoint order) {
        this.order = order;
    }

    public ServiceEndpoint getPayment() {
        return payment;
    }

    public void setPayment(ServiceEndpoint payment) {
        this.payment = payment;
    }

    public ServiceEndpoint getRefund() {
        return refund;
    }

    public void setRefund(ServiceEndpoint refund) {
        this.refund = refund;
    }

    public ServiceEndpoint getAnalytics() {
        return analytics;
    }

    public void setAnalytics(ServiceEndpoint analytics) {
        this.analytics = analytics;
    }

    public ServiceEndpoint getBackend() {
        return backend;
    }

    public void setBackend(ServiceEndpoint backend) {
        this.backend = backend;
    }

    public static class ServiceEndpoint {
        private String baseUrl;

        public ServiceEndpoint() {
        }

        public ServiceEndpoint(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }
}
