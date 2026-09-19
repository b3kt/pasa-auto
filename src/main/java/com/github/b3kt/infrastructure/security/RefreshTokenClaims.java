package com.github.b3kt.infrastructure.security;

/**
 * Verified claims of a refresh token: its owner and its server-side id ({@code jti}).
 */
public record RefreshTokenClaims(String username, String tokenId) {
}
