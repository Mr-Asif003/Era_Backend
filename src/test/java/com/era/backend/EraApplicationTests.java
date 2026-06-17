package com.era.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class EraApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the Spring context wires up correctly.
        // Requires MongoDB, Redis, and Kafka to be reachable - run
        // `docker compose -f docker-compose.dev.yml up -d` first.
    }
}
