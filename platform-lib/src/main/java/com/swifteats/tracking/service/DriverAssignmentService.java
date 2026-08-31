package com.swifteats.tracking.service;

import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;

import com.swifteats.common.domain.DriverStatus;
import com.swifteats.tracking.exception.NoDriverAvailableException;
import com.swifteats.tracking.entity.Driver;
import com.swifteats.tracking.repository.DriverRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@ServiceScope({ServiceName.ORDER, ServiceName.BACKEND})
public class DriverAssignmentService {

    private final DriverRepository driverRepository;
    private final DriverLocationCacheService locationCacheService;

    public DriverAssignmentService(
            DriverRepository driverRepository,
            DriverLocationCacheService locationCacheService) {
        this.driverRepository = driverRepository;
        this.locationCacheService = locationCacheService;
    }

    @Transactional
    public UUID assignDriver(UUID orderId) {
        Driver driver = driverRepository.findFirstByStatusOrderByCreatedAtAsc(DriverStatus.AVAILABLE)
                .orElseThrow(NoDriverAvailableException::new);
        driver.setStatus(DriverStatus.ON_DELIVERY);
        driver.setCurrentOrderId(orderId);
        driverRepository.save(driver);
        locationCacheService.cacheOrderDriver(orderId, driver.getId());
        return driver.getId();
    }

    @Transactional
    public void releaseDriver(UUID driverId) {
        driverRepository.findById(driverId).ifPresent(driver -> {
            driver.setStatus(DriverStatus.AVAILABLE);
            driver.setCurrentOrderId(null);
            driverRepository.save(driver);
        });
    }
}
