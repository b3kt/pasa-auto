package com.github.b3kt.infrastructure.health;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.Readiness;

import javax.sql.DataSource;
import java.sql.Connection;

@Liveness
@Readiness
@ApplicationScoped
public class DatabaseHealthCheck implements HealthCheck {

    @Inject
    DataSource dataSource;

    @Override
    public HealthCheckResponse call() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(2)) {
                return HealthCheckResponse.named("Database Connection")
                        .up()
                        .withData("database", "PostgreSQL")
                        .build();
            } else {
                return HealthCheckResponse.named("Database Connection")
                        .down()
                        .withData("error", "Connection not valid")
                        .build();
            }
        } catch (Exception e) {
            return HealthCheckResponse.named("Database Connection")
                    .down()
                    .withData("error", e.getMessage())
                    .build();
        }
    }
}