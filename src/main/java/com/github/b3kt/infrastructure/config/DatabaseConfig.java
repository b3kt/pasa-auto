package com.github.b3kt.infrastructure.config;

import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Configuration for database repository selection.
 * Note: Repository selection is handled via CDI @Alternative annotation
 * or by excluding one of the repository implementations.
 */
public class DatabaseConfig {

    @ConfigProperty(name = "app.repository.type", defaultValue = "jpa")
    String repositoryType;

    /**
     * Determines which repository implementation to use.
     * Default is JPA (database), can be switched to "memory" for in-memory.
     */
    public boolean useJpaRepository() {
        return "jpa".equalsIgnoreCase(repositoryType);
    }
}

