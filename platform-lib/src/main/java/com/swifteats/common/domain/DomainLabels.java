package com.swifteats.common.domain;

/**
 * Shared vocabulary aligned with Assignment 2 sample CSV literals.
 * Live platform and analytics correlation both use these labels.
 */
public final class DomainLabels {

    private DomainLabels() {
    }

    public static final class FailureReason {
        public static final String STOCKOUT = "Stockout";
        public static final String WAREHOUSE_DELAY = "Warehouse delay";
        public static final String TRAFFIC_CONGESTION = "Traffic congestion";
        public static final String INCORRECT_ADDRESS = "Incorrect address";
        public static final String WEATHER_DISRUPTION = "Weather disruption";
        public static final String PAYMENT_FAILED = "Payment failed";
        public static final String DRIVER_UNAVAILABLE = "Driver unavailable";

        private FailureReason() {
        }
    }

    public static final class KitchenNote {
        public static final String STOCK_DELAY = "Stock delay on item";
        public static final String SLOW_PACKING = "Slow packing";
        public static final String SYSTEM_ISSUE = "System issue";

        private KitchenNote() {
        }
    }

    public static final class FleetNote {
        public static final String ADDRESS_NOT_FOUND = "Address not found";
        public static final String HEAVY_CONGESTION = "Heavy congestion";
        public static final String BREAKDOWN = "Breakdown";

        private FleetNote() {
        }
    }

    public static final class Traffic {
        public static final String CLEAR = "Clear";
        public static final String MODERATE = "Moderate";
        public static final String HEAVY = "Heavy";

        private Traffic() {
        }
    }

    public static final class Weather {
        public static final String CLEAR = "Clear";
        public static final String RAIN = "Rain";
        public static final String FOG = "Fog";

        private Weather() {
        }
    }

    public static final class ExternalEvent {
        public static final String STRIKE = "Strike";
        public static final String HOLIDAY = "Holiday";
        public static final String FESTIVAL = "Festival";

        private ExternalEvent() {
        }
    }
}
