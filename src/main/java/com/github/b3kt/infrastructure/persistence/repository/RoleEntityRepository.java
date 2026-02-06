package com.github.b3kt.infrastructure.persistence.repository;

import com.github.b3kt.infrastructure.persistence.entity.RoleEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

/**
 * JPA Repository for RoleEntity using Panache.
 */
@ApplicationScoped
public class RoleEntityRepository implements PanacheRepository<RoleEntity> {

    /**
     * Find role by name.
     */
    public Optional<RoleEntity> findByName(String name) {
        return find("name", name).firstResultOptional();
    }

    /**
     * Check if role exists by name.
     */
    public boolean existsByName(String name) {
        return count("name", name) > 0;
    }

    /**
     * Find all active roles.
     */
    public List<RoleEntity> findActiveRoles() {
        return find("active", true).list();
    }

    /**
     * Find role by name and active status.
     */
    public Optional<RoleEntity> findByNameAndActive(String name, boolean active) {
        return find("name = ?1 and active = ?2", name, active).firstResultOptional();
    }
}

