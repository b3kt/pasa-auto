package com.github.b3kt.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for creating a permission.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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
}

