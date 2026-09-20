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

    /**
     * Find the user bound to a Google account. Once set, this is the authoritative match - an
     * email address can change, the subject cannot.
     */
    public Optional<UserEntity> findByGoogleSub(String googleSub) {
        return find("googleSub", googleSub).firstResultOptional();
    }

    /**
     * Users with this email address, compared case-insensitively.
     *
     * <p>Returns a list rather than an Optional on purpose: email carries no unique constraint in
     * this schema, so more than one row can match, and a caller matching an identity must refuse
     * that case rather than pick one.
     */
    public java.util.List<UserEntity> findByEmailIgnoreCase(String email) {
        return list("lower(email) = ?1", email.toLowerCase());
    }
}

