package com.swifteats.tracking.service;

import com.swifteats.common.domain.DriverStatus;
import com.swifteats.tracking.entity.Driver;
import com.swifteats.tracking.exception.NoDriverAvailableException;
import com.swifteats.tracking.repository.DriverRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverAssignmentServiceTest {

    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID DRIVER_ID = UUID.randomUUID();

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private DriverLocationCacheService locationCacheService;

    private DriverAssignmentService service;

    @BeforeEach
    void setUp() {
        service = new DriverAssignmentService(driverRepository, locationCacheService);
    }

    @Test
    void assignDriver_assignsFirstAvailableDriver() {
        Driver driver = availableDriver();
        when(driverRepository.findFirstByStatusOrderByCreatedAtAsc(DriverStatus.AVAILABLE))
                .thenReturn(Optional.of(driver));
        when(driverRepository.save(any(Driver.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UUID assigned = service.assignDriver(ORDER_ID);

        assertThat(assigned).isEqualTo(DRIVER_ID);
        ArgumentCaptor<Driver> saved = ArgumentCaptor.forClass(Driver.class);
        verify(driverRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(DriverStatus.ON_DELIVERY);
        assertThat(saved.getValue().getCurrentOrderId()).isEqualTo(ORDER_ID);
        verify(locationCacheService).cacheOrderDriver(ORDER_ID, DRIVER_ID);
    }

    @Test
    void assignDriver_throwsWhenNoDriverAvailable() {
        when(driverRepository.findFirstByStatusOrderByCreatedAtAsc(DriverStatus.AVAILABLE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignDriver(ORDER_ID))
                .isInstanceOf(NoDriverAvailableException.class);
        verify(locationCacheService, never()).cacheOrderDriver(any(), any());
    }

    @Test
    void releaseDriver_marksDriverAvailableAndClearsOrder() {
        Driver driver = new Driver();
        driver.setId(DRIVER_ID);
        driver.setStatus(DriverStatus.ON_DELIVERY);
        driver.setCurrentOrderId(ORDER_ID);
        when(driverRepository.findById(DRIVER_ID)).thenReturn(Optional.of(driver));
        when(driverRepository.save(any(Driver.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.releaseDriver(DRIVER_ID);

        ArgumentCaptor<Driver> saved = ArgumentCaptor.forClass(Driver.class);
        verify(driverRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(DriverStatus.AVAILABLE);
        assertThat(saved.getValue().getCurrentOrderId()).isNull();
    }

    @Test
    void releaseDriver_noOpWhenDriverMissing() {
        when(driverRepository.findById(DRIVER_ID)).thenReturn(Optional.empty());

        service.releaseDriver(DRIVER_ID);

        verify(driverRepository, never()).save(any());
    }

    private static Driver availableDriver() {
        Driver driver = new Driver();
        driver.setId(DRIVER_ID);
        driver.setStatus(DriverStatus.AVAILABLE);
        return driver;
    }
}
