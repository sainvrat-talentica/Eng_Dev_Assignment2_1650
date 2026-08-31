package com.swifteats.analytics.engine;

import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;

import com.swifteats.analytics.model.CorrelationResult;
import com.swifteats.analytics.model.CorrelationRuleMatch;
import com.swifteats.analytics.model.EnrichedOrder;
import com.swifteats.common.domain.DomainLabels;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ServiceScope(ServiceName.ANALYTICS)
public class CorrelationEngine {

    public CorrelationResult correlate(EnrichedOrder row) {
        List<CorrelationRuleMatch> matched = new ArrayList<>();

        if (DomainLabels.FailureReason.STOCKOUT.equals(row.failureReason())
                && contains(row.warehouseNotes(), "Stock delay")) {
            matched.add(rule("STOCKOUT_WAREHOUSE", 0.9));
        }

        if (DomainLabels.FailureReason.WAREHOUSE_DELAY.equals(row.failureReason())
                && matchesAny(row.warehouseNotes(),
                DomainLabels.KitchenNote.SLOW_PACKING,
                DomainLabels.KitchenNote.SYSTEM_ISSUE)) {
            matched.add(rule("WAREHOUSE_OPS", 0.85));
        }

        if (DomainLabels.FailureReason.TRAFFIC_CONGESTION.equals(row.failureReason())
                && DomainLabels.FleetNote.HEAVY_CONGESTION.equals(row.gpsDelayNotes())
                && DomainLabels.Traffic.HEAVY.equals(row.trafficCondition())) {
            matched.add(rule("TRAFFIC_TRIPLE_CONFIRM", 0.95));
        }

        if (DomainLabels.FailureReason.INCORRECT_ADDRESS.equals(row.failureReason())
                && DomainLabels.FleetNote.ADDRESS_NOT_FOUND.equals(row.gpsDelayNotes())) {
            matched.add(rule("ADDRESS_MISMATCH", 0.9));
        }

        if (DomainLabels.FailureReason.WEATHER_DISRUPTION.equals(row.failureReason())
                && matchesAny(row.weatherCondition(), DomainLabels.Weather.RAIN, DomainLabels.Weather.FOG)) {
            matched.add(rule("WEATHER_IMPACT", 0.8));
        }

        if (row.delayed() && !row.failed()) {
            matched.add(rule("SLA_BREACH", 0.7));
        }

        if (DomainLabels.ExternalEvent.FESTIVAL.equals(row.eventType())
                || DomainLabels.ExternalEvent.HOLIDAY.equals(row.eventType())) {
            matched.add(rule("FESTIVAL_VOLUME", 0.75));
        }

        return new CorrelationResult(matched);
    }

    private static CorrelationRuleMatch rule(String id, double confidence) {
        return new CorrelationRuleMatch(id, confidence);
    }

    private static boolean contains(String value, String fragment) {
        return value != null && value.contains(fragment);
    }

    private static boolean matchesAny(String value, String... candidates) {
        if (value == null) {
            return false;
        }
        for (String candidate : candidates) {
            if (value.contains(candidate)) {
                return true;
            }
        }
        return false;
    }
}
