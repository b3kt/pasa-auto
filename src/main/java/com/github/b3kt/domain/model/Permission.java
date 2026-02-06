package com.github.b3kt.domain.model;

/**
 * Domain entity representing a Permission in RBAC system.
 */
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

