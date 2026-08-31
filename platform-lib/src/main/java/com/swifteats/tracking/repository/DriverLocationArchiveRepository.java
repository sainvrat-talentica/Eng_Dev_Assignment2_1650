package com.swifteats.tracking.repository;

import com.swifteats.tracking.entity.DriverLocationArchive;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverLocationArchiveRepository extends JpaRepository<DriverLocationArchive, Long> {
}
