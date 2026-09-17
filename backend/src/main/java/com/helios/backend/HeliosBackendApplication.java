package com.helios.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * HELIOS backend entrypoint.
 *
 * Modular monolith bootstrap only (Phase 0). Feature modules
 * (identity, patient, documents, observations, timeline, ...)
 * are added in subsequent phases per the HELIOS implementation order.
 */
@SpringBootApplication
public class HeliosBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(HeliosBackendApplication.class, args);
    }
}
