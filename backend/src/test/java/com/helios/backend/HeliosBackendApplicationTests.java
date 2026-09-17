package com.helios.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifies the Spring application context loads successfully.
 * Requires a reachable PostgreSQL instance matching backend/.env
 * (see README for local setup) because spring-boot-starter-data-jpa
 * initializes a real DataSource at context startup.
 */
@SpringBootTest
class HeliosBackendApplicationTests {

    @Test
    void contextLoads() {
        // Intentionally empty: failure to load the context fails this test.
    }
}
