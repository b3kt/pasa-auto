package com.github.b3kt.domain.model;

import java.util.HashSet;
import java.util.Set;

/**
 * Domain entity representing a Role in RBAC system.
 */
public class Role {
    private Long id;
    private String name;
    private String description;
    private Set<Permission> permissions;
    private boolean active;

    public Role() {
        this.permissions = new HashSet<>();
        this.active = true;
    }

    public Role(String name, String description) {
        this();
        this.name = name;
        this.description = description;
    }

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

    public Set<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<Permission> permissions) {
        this.permissions = permissions != null ? new HashSet<>(permissions) : new HashSet<>();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Business logic: Check if role has a specific permission
     */
    public boolean hasPermission(String permissionName) {
        return permissions.stream()
                .anyMatch(p -> p.getName().equals(permissionName) && p.isActive());
    }

    /**
     * Business logic: Add permission to role
     */
    public void addPermission(Permission permission) {
        if (permission != null) {
            this.permissions.add(permission);
        }
    }

    /**
     * Business logic: Remove permission from role
     */
    public void removePermission(Permission permission) {
        if (permission != null) {
            this.permissions.remove(permission);
        }
    }
}

