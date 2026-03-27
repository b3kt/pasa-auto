package com.github.b3kt.domain.model;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;

/**
 * Domain entity representing a Role in RBAC system.
 */
@Data
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

    public void setPermissions(Set<Permission> permissions) {
        this.permissions = permissions != null ? new HashSet<>(permissions) : new HashSet<>();
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

