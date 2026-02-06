package com.github.b3kt.infrastructure.persistence.entity;

import com.github.b3kt.domain.model.Permission;
import jakarta.persistence.*;

/**
 * JPA Entity for Permission.
 */
@Entity
@Table(name = "permissions", indexes = {
    @Index(name = "idx_permission_name", columnList = "name", unique = true),
    @Index(name = "idx_permission_resource_action", columnList = "resource,action")
})
public class PermissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(nullable = false, length = 50)
    private String resource;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(nullable = false)
    private boolean active = true;

    public PermissionEntity() {
    }

    public PermissionEntity(String name, String description, String resource, String action) {
        this.name = name;
        this.description = description;
        this.resource = resource;
        this.action = action;
        this.active = true;
    }

    /**
     * Convert to domain Permission entity.
     */
    public Permission toDomain() {
        Permission permission = new Permission();
        permission.setId(this.id);
        permission.setName(this.name);
        permission.setDescription(this.description);
        permission.setResource(this.resource);
        permission.setAction(this.action);
        permission.setActive(this.active);
        return permission;
    }

    /**
     * Create from domain Permission entity.
     */
    public static PermissionEntity fromDomain(Permission permission) {
        PermissionEntity entity = new PermissionEntity();
        entity.setId(permission.getId());
        entity.setName(permission.getName());
        entity.setDescription(permission.getDescription());
        entity.setResource(permission.getResource());
        entity.setAction(permission.getAction());
        entity.setActive(permission.isActive());
        return entity;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}

