package com.swifteats.analytics.engine;

import com.swifteats.analytics.model.CorrelationResult;
import com.swifteats.analytics.model.EnrichedOrder;
import com.swifteats.common.domain.DomainLabels;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationEngineTest {

    private final CorrelationEngine engine = new CorrelationEngine();

    @Test
    void correlatesStockoutWithWarehouseStockDelay() {
        EnrichedOrder order = sample(
                DomainLabels.FailureReason.STOCKOUT,
                "Stock delay on item",
                null, null, null, null, false, true);

        CorrelationResult result = engine.correlate(order);

        assertThat(result.matches()).extracting(m -> m.ruleId()).contains("STOCKOUT_WAREHOUSE");
    }

    @Test
    void correlatesTrafficTripleConfirm() {
        EnrichedOrder order = sample(
                DomainLabels.FailureReason.TRAFFIC_CONGESTION,
                null,
                DomainLabels.FleetNote.HEAVY_CONGESTION,
                DomainLabels.Traffic.HEAVY,
                null,
                null,
                false,
                true);

        CorrelationResult result = engine.correlate(order);

        assertThat(result.matches()).extracting(m -> m.ruleId()).contains("TRAFFIC_TRIPLE_CONFIRM");
    }

    @Test
    void correlatesSlaBreachForDelayedNonFailedOrders() {
        EnrichedOrder order = sample(null, null, null, null, null, null, true, false);

        CorrelationResult result = engine.correlate(order);

        assertThat(result.matches()).extracting(m -> m.ruleId()).contains("SLA_BREACH");
    }

    @Test
    void correlatesWarehouseOpsWhenPackingIsSlow() {
        EnrichedOrder order = sample(
                DomainLabels.FailureReason.WAREHOUSE_DELAY,
                DomainLabels.KitchenNote.SLOW_PACKING,
                null, null, null, null, false, true);

        CorrelationResult result = engine.correlate(order);

        assertThat(result.matches()).extracting(m -> m.ruleId()).contains("WAREHOUSE_OPS");
    }

    @Test
    void correlatesAddressMismatchWhenGpsCannotFindAddress() {
        EnrichedOrder order = sample(
                DomainLabels.FailureReason.INCORRECT_ADDRESS,
                null,
                DomainLabels.FleetNote.ADDRESS_NOT_FOUND,
                null, null, null, false, true);

        CorrelationResult result = engine.correlate(order);

        assertThat(result.matches()).extracting(m -> m.ruleId()).contains("ADDRESS_MISMATCH");
    }

    @Test
    void correlatesWeatherImpactDuringRain() {
        EnrichedOrder order = sample(
                DomainLabels.FailureReason.WEATHER_DISRUPTION,
                null, null, null,
                DomainLabels.Weather.RAIN,
                null, false, true);

        CorrelationResult result = engine.correlate(order);

        assertThat(result.matches()).extracting(m -> m.ruleId()).contains("WEATHER_IMPACT");
    }

    @Test
    void correlatesFestivalVolumeDuringHolidayEvents() {
        EnrichedOrder order = sample(
                null, null, null, null, null,
                DomainLabels.ExternalEvent.HOLIDAY,
                false, false);

        CorrelationResult result = engine.correlate(order);

        assertThat(result.matches()).extracting(m -> m.ruleId()).contains("FESTIVAL_VOLUME");
    }

    private static EnrichedOrder sample(
            String failureReason,
            String warehouseNotes,
            String gpsNotes,
            String traffic,
            String weather,
            String eventType,
            boolean delayed,
            boolean failed) {
        return new EnrichedOrder(
                1L, "Pune", failed ? "Failed" : "Delivered", failureReason,
                delayed, failed, 1L, "Test Client", 1L, "WH-1",
                warehouseNotes, gpsNotes, 1L, traffic, weather, eventType,
                null, null, null);
    }
}
