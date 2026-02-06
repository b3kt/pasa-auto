package com.github.b3kt.infrastructure.persistence.repository;

import com.github.b3kt.infrastructure.persistence.entity.PermissionEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

/**
 * JPA Repository for PermissionEntity using Panache.
 */
@ApplicationScoped
public class PermissionEntityRepository implements PanacheRepository<PermissionEntity> {

    /**
     * Find permission by name.
     */
    public Optional<PermissionEntity> findByName(String name) {
        return find("name", name).firstResultOptional();
    }

    /**
     * Check if permission exists by name.
     */
    public boolean existsByName(String name) {
        return count("name", name) > 0;
    }

    /**
     * Find all active permissions.
     */
    public List<PermissionEntity> findActivePermissions() {
        return find("active", true).list();
    }

    /**
     * Find permissions by resource.
     */
    public List<PermissionEntity> findByResource(String resource) {
        return find("resource", resource).list();
    }

    /**
     * Find permission by resource and action.
     */
    public Optional<PermissionEntity> findByResourceAndAction(String resource, String action) {
        return find("resource = ?1 and action = ?2", resource, action).firstResultOptional();
    }

    /**
     * Find permission by name and active status.
     */
    public Optional<PermissionEntity> findByNameAndActive(String name, boolean active) {
        return find("name = ?1 and active = ?2", name, active).firstResultOptional();
    }
}

