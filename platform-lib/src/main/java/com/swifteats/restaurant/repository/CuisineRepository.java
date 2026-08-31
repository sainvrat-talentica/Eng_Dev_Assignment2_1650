package com.swifteats.restaurant.repository;

import com.swifteats.restaurant.entity.Cuisine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CuisineRepository extends JpaRepository<Cuisine, UUID> {

    Optional<Cuisine> findByName(String name);

    List<Cuisine> findByNameIn(Collection<String> names);
}
