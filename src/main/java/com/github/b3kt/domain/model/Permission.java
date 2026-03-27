package com.github.b3kt.domain.model;

import lombok.Data;

/**
 * Domain entity representing a Permission in RBAC system.
 */
@Data
public class Permission {
    private Long id;
    private String name;
    private String description;
    private String resource;
    private String action;
    private boolean active;

    public Permission() {
        this.active = true;
    }

    public Permission(String name, String description, String resource, String action) {
        this();
        this.name = name;
        this.description = description;
        this.resource = resource;
        this.action = action;
    }

    /**
     * Business logic: Check if permission is valid
     */
    public boolean isValid() {
        return name != null && !name.isEmpty() 
            && resource != null && !resource.isEmpty()
            && action != null && !action.isEmpty()
            && active;
    }
}

