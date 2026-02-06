package com.github.b3kt.application.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * DTO for refresh token request.
 */
@Schema(description = "Request to refresh access token using refresh token")
public class RefreshTokenRequest {
    
    @Schema(description = "The refresh token", required = true)
    private String refreshToken;
    
    public RefreshTokenRequest() {
    }
    
    public RefreshTokenRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }
    
    public String getRefreshToken() {
        return refreshToken;
    }
    
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
