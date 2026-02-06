package com.github.b3kt.infrastructure.persistence.repository;

import com.github.b3kt.infrastructure.persistence.entity.UserEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

/**
 * JPA Repository for UserEntity using Panache.
 */
@ApplicationScoped
public class UserEntityRepository implements PanacheRepository<UserEntity> {

    /**
     * Find user by username.
     */
    public Optional<UserEntity> findByUsername(String username) {
        return find("username", username).firstResultOptional();
    }

    /**
     * Check if user exists by username.
     */
    public boolean existsByUsername(String username) {
        return count("username", username) > 0;
    }
}

