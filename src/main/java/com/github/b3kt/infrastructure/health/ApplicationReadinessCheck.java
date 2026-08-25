package com.github.b3kt.infrastructure.health;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;

@Readiness
@ApplicationScoped
public class ApplicationReadinessCheck implements HealthCheck {

    @Inject
    DataSource dataSource;

    @Override
    public HealthCheckResponse call() {
        try (Connection connection = dataSource.getConnection()) {
            // Check if migrations are applied
            try (ResultSet rs = connection.createStatement().executeQuery(
                    "SELECT version FROM flyway_schema_history WHERE success = true ORDER BY installed_rank DESC LIMIT 1")) {
                if (rs.next()) {
                    String version = rs.getString("version");
                    return HealthCheckResponse.named("Application Readiness")
                            .up()
                            .withData("migration_version", version)
                            .withData("status", "ready")
                            .build();
                }
            }
            return HealthCheckResponse.named("Application Readiness")
                    .down()
                    .withData("error", "No successful migrations found")
                    .build();
        } catch (Exception e) {
            return HealthCheckResponse.named("Application Readiness")
                    .down()
                    .withData("error", e.getMessage())
                    .build();
        }
    }
}