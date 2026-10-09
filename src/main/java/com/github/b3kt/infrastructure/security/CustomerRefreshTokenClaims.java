package com.github.b3kt.infrastructure.security;

/**
 * Verified claims of a customer refresh token: the account it belongs to and its server-side id ({@code jti}).
 */
public record CustomerRefreshTokenClaims(Long customerAccountId, String tokenId) {
}
