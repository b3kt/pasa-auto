package com.github.b3kt.application.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * DTO for login response containing JWT token and user info.
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Login response containing JWT token and user information")
public class LoginResponse {

    @Schema(description = "JWT authentication token", example = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;

    @Schema(description = "Username", example = "admin")
    private String username;

    @Schema(description = "User email address", example = "admin@example.com")
    private String email;

    @Schema(description = "Token expiration time in seconds", example = "86400")
    private Long expiresIn;

    @Schema(description = "Refresh token for obtaining new access tokens", example = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String refreshToken;

    public LoginResponse(String token, String username, String email, Long expiresIn) {
        this.token = token;
        this.username = username;
        this.email = email;
        this.expiresIn = expiresIn;
    }

    public LoginResponse(String token, String refreshToken, String username, String email, Long expiresIn) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.username = username;
        this.email = email;
        this.expiresIn = expiresIn;
    }
}

