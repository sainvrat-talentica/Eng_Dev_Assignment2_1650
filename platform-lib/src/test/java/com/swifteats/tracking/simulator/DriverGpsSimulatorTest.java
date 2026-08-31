package com.swifteats.tracking.simulator;

import com.swifteats.tracking.config.TrackingProperties;
import com.swifteats.tracking.dto.LocationUpdateRequest;
import com.swifteats.tracking.entity.Driver;
import com.swifteats.tracking.repository.DriverRepository;
import com.swifteats.tracking.service.GpsIngestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverGpsSimulatorTest {

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private GpsIngestService gpsIngestService;

    private TrackingProperties properties;
    private DriverGpsSimulator simulator;

    @BeforeEach
    void setUp() {
        properties = new TrackingProperties();
        properties.setSimulatorDriverCount(2);
        simulator = new DriverGpsSimulator(driverRepository, gpsIngestService, properties);
    }

    @Test
    void init_limitsDriversToConfiguredCount() {
        when(driverRepository.findAll()).thenReturn(List.of(driver(UUID.randomUUID()), driver(UUID.randomUUID()), driver(UUID.randomUUID())));

        ReflectionTestUtils.invokeMethod(simulator, "init");

        assertThat(simStates()).hasSize(2);
    }

    @Test
    void tick_callsIngestForEachSimulatedDriver() {
        UUID driverOne = UUID.randomUUID();
        UUID driverTwo = UUID.randomUUID();
        when(driverRepository.findAll()).thenReturn(List.of(driver(driverOne), driver(driverTwo)));
        ReflectionTestUtils.invokeMethod(simulator, "init");

        ReflectionTestUtils.invokeMethod(simulator, "tick");

        verify(gpsIngestService, atLeastOnce()).ingest(eq(driverOne), any(LocationUpdateRequest.class));
        verify(gpsIngestService, atLeastOnce()).ingest(eq(driverTwo), any(LocationUpdateRequest.class));
    }

    @Test
    void tick_ingestsDriftedCoordinates() {
        UUID driverId = UUID.randomUUID();
        when(driverRepository.findAll()).thenReturn(List.of(driver(driverId)));
        ReflectionTestUtils.invokeMethod(simulator, "init");

        ReflectionTestUtils.invokeMethod(simulator, "tick");

        ArgumentCaptor<LocationUpdateRequest> captor = ArgumentCaptor.forClass(LocationUpdateRequest.class);
        verify(gpsIngestService).ingest(eq(driverId), captor.capture());
        LocationUpdateRequest request = captor.getValue();
        assertThat(request.latitude()).isNotNull();
        assertThat(request.longitude()).isNotNull();
        assertThat(request.heading()).isNotNull();
    }

    @SuppressWarnings("unchecked")
    private Map<UUID, ?> simStates() {
        return (ConcurrentHashMap<UUID, ?>) ReflectionTestUtils.getField(simulator, "states");
    }

    private static Driver driver(UUID id) {
        Driver driver = new Driver();
        driver.setId(id);
        driver.setName("Driver-" + id.toString().substring(0, 4));
        return driver;
    }
}
