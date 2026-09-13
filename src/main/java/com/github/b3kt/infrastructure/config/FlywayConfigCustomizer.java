package com.github.b3kt.infrastructure.config;

import io.quarkus.flyway.FlywayConfigurationCustomizer;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.flywaydb.core.api.configuration.FluentConfiguration;

/**
 * Customizes the Flyway configuration to be resilient to PostgreSQL
 * advisory-lock contention (e.g. during dev-mode live reload restarts).
 */
@ApplicationScoped
public class FlywayConfigCustomizer implements FlywayConfigurationCustomizer {

    @ConfigProperty(name = "app.flyway.lock-retry-count", defaultValue = "2000")
    int lockRetryCount;

    @Override
    public void customize(FluentConfiguration configuration) {
        configuration.lockRetryCount(lockRetryCount);
    }
}