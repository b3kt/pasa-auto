package com.github.b3kt.application.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * DTO for refresh token request.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to refresh access token using refresh token")
public class RefreshTokenRequest {

    @Schema(description = "The refresh token", required = true)
    private String refreshToken;
}
