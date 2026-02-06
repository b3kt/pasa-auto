package com.github.b3kt.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for creating a role.
 */
public class CreateRoleRequest {
    
    @NotBlank(message = "Role name harus diisi")
    @Size(max = 50, message = "Role name must not exceed 50 characters")
    private String name;
    
    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    public CreateRoleRequest() {
    }

    public CreateRoleRequest(String name, String description) {
        this.name = name;
        this.description = description;
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
}

