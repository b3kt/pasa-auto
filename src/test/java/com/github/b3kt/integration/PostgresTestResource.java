package com.github.b3kt.integration;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Collections;
import java.util.Map;

public class PostgresTestResource implements QuarkusTestResourceLifecycleManager {

    private static PostgreSQLContainer<?> postgres;

    @Override
    public Map<String, String> start() {
        postgres = new PostgreSQLContainer<>("postgres:15-alpine")
                .withDatabaseName("pasa_auto_test")
                .withUsername("test")
                .withPassword("test");
        
        postgres.start();
        
        return Collections.singletonMap(
            "quarkus.datasource.jdbc.url", 
            postgres.getJdbcUrl()
        );
    }

    @Override
    public void stop() {
        if (postgres != null) {
            postgres.stop();
        }
    }
}
