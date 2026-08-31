package com.swifteats.auth;

import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;

import com.swifteats.order.entity.Customer;
import com.swifteats.order.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ServiceScope(ServiceName.BACKEND)
public class DemoCustomerBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoCustomerBootstrap.class);
    private static final UUID DEMO_CUSTOMER_ID = UUID.fromString("44444444-4444-4444-4444-444444444401");
    private static final String DEMO_PASSWORD = "Demo@123";

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoCustomerBootstrap(CustomerRepository customerRepository, PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        customerRepository.findById(DEMO_CUSTOMER_ID).ifPresent(customer -> {
            if (customer.getPasswordHash() != null) {
                return;
            }
            customer.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
            customerRepository.save(customer);
            log.info("Demo customer password initialized (login: demo.customer@example.com / Demo@123)");
        });
    }
}
