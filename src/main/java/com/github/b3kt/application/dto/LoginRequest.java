package com.github.b3kt.application.dto;

import jakarta.validation.constraints.NotBlank;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * DTO for login request.
 */
@Schema(description = "Login request with username and password")
public class LoginRequest {
    
    @NotBlank(message = "Username harus diisi")
    @Schema(description = "Username for authentication", example = "admin", required = true)
    private String username;
    
    @NotBlank(message = "Password harus diisi")
    @Schema(description = "Password for authentication", example = "admin123", required = true)
    private String password;

    public LoginRequest() {
    }

    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}

