package com.github.b3kt.integration;

import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public abstract class IntegrationTestBase {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("pasa_auto_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    @BeforeEach
    void setUp() {
        // Common test setup
    }
}
