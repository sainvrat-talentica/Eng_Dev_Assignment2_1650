package com.swifteats.tracking.repository;

import com.swifteats.common.domain.DriverStatus;
import com.swifteats.tracking.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DriverRepository extends JpaRepository<Driver, UUID> {

    Optional<Driver> findFirstByStatusOrderByCreatedAtAsc(DriverStatus status);

    boolean existsByIdAndApiToken(UUID id, String apiToken);
}
