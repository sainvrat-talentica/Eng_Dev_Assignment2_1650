package com.swifteats;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

/**
 * Minimal config for {@code @WebMvcTest} slices — no component scan of production beans.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
public class WebMvcTestConfig {
}
