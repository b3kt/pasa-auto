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
     * @return the refresh token string
     */
    String generateRefreshToken(User user);
    
    /**
     * Validate a refresh token and return the username.
     * 
     * @param refreshToken the refresh token to validate
     * @return the username from the token, or null if invalid
     */
    String validateRefreshToken(String refreshToken);
}

