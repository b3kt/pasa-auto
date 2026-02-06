package com.github.b3kt.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for creating a permission.
 */
public class CreatePermissionRequest {
    
    @NotBlank(message = "Permission name harus diisi")
    @Size(max = 100, message = "Permission name must not exceed 100 characters")
    private String name;
    
    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;
    
    @NotBlank(message = "Resource harus diisi")
    @Size(max = 50, message = "Resource must not exceed 50 characters")
    private String resource;
    
    @NotBlank(message = "Action harus diisi")
    @Size(max = 50, message = "Action must not exceed 50 characters")
    private String action;

    public CreatePermissionRequest() {
    }

    public CreatePermissionRequest(String name, String description, String resource, String action) {
        this.name = name;
        this.description = description;
        this.resource = resource;
        this.action = action;
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
}

