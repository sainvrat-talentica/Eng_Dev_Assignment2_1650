package com.swifteats.order.repository;

import com.swifteats.common.domain.CustomerStatus;
import com.swifteats.order.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    boolean existsByIdAndApiToken(UUID id, String apiToken);

    Optional<Customer> findByIdAndApiToken(UUID id, String apiToken);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPhone(String phone);

    Optional<Customer> findByEmailIgnoreCase(String email);

    Optional<Customer> findByPhone(String phone);

    List<Customer> findAllByOrderByCreatedAtDesc();

    List<Customer> findByStatusOrderByCreatedAtDesc(CustomerStatus status);
}
