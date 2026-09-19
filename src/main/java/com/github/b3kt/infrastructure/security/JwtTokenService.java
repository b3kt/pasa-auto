package com.github.b3kt.infrastructure.security;

import com.github.b3kt.domain.model.User;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * Service interface for JWT token operations.
 */
public interface JwtTokenService {
    
    /**
     * Generate JWT token for a user.
     * 
     * @param user the user
     * @return the JWT token string
     */
    String generateToken(User user);
    
    /**
     * Extract user information from JWT token.
     * 
     * @param jwt the JSON Web Token
     * @return UserInfo extracted from token
     */
    com.github.b3kt.application.dto.UserInfo extractUserInfo(JsonWebToken jwt);
    
    /**
     * Get token expiration time in seconds.
     * 
     * @return expiration time in seconds
     */
    long getTokenExpirationSeconds();
    
    /**
     * Generate a refresh token for a user.
     * 
     * @param user the user
     * @param tokenId the server-side id of the token, stored as its {@code jti} claim
     * @return the refresh token string
     */
    String generateRefreshToken(User user, String tokenId);
    
    /**
     * Get how long a refresh token stays valid.
     * 
     * @return refresh token lifetime
     */
    java.time.Duration getRefreshTokenLifetime();
    
    /**
     * Verify a refresh token's signature, issuer, expiry and type.
     * This does not check whether the token was rotated or revoked.
     * 
     * @param refreshToken the refresh token to validate
     * @return the token's username and id, or null if invalid
     */
    RefreshTokenClaims validateRefreshToken(String refreshToken);
}

