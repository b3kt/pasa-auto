package com.github.b3kt.infrastructure.persistence.repository;

import com.github.b3kt.infrastructure.persistence.entity.SystemParameterEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

/**
 * JPA Repository for SystemParameterEntity using Panache.
 */
@ApplicationScoped
public class SystemParameterEntityRepository implements PanacheRepository<SystemParameterEntity> {

    /**
     * Find system parameter by name.
     */
    public Optional<SystemParameterEntity> findByName(String name) {
        return find("name", name).firstResultOptional();
    }

    /**
     * Check if system parameter exists by name.
     */
    public boolean existsByName(String name) {
        return count("name", name) > 0;
    }
}
