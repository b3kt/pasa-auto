package com.github.b3kt.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseConfigTest {

    @Test
    @DisplayName("useJpaRepository returns true for 'jpa'")
    void useJpaRepositoryJpa() {
        DatabaseConfig config = new DatabaseConfig();
        config.repositoryType = "jpa";
        assertTrue(config.useJpaRepository());
    }

    @Test
    @DisplayName("useJpaRepository returns true for 'JPA' (case insensitive)")
    void useJpaRepositoryJpaCaseInsensitive() {
        DatabaseConfig config = new DatabaseConfig();
        config.repositoryType = "JPA";
        assertTrue(config.useJpaRepository());
    }

    @Test
    @DisplayName("useJpaRepository returns false for 'memory'")
    void useJpaRepositoryMemory() {
        DatabaseConfig config = new DatabaseConfig();
        config.repositoryType = "memory";
        assertFalse(config.useJpaRepository());
    }

    @Test
    @DisplayName("useJpaRepository returns true for unknown values (default jpa)")
    void useJpaRepositoryDefault() {
        DatabaseConfig config = new DatabaseConfig();
        config.repositoryType = "other";
        assertFalse(config.useJpaRepository());
    }
}
