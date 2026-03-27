package com.github.b3kt.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * DTO for login request.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Login request with username and password")
public class LoginRequest {

    @NotBlank(message = "Username harus diisi")
    @Schema(description = "Username for authentication", example = "admin", required = true)
    private String username;

    @NotBlank(message = "Password harus diisi")
    @Schema(description = "Password for authentication", example = "admin123", required = true)
    private String password;
}

