package com.github.b3kt.application.service;

import com.github.b3kt.application.dto.LoginResponse;
import com.github.b3kt.application.dto.UserInfo;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * Application service interface for authentication operations.
 * This defines the use cases for authentication.
 */
public interface AuthService {
    
    /**
     * Authenticate user and generate JWT token.
     * 
     * @param username the username
     * @param password the password
     * @return LoginResponse with JWT token and user info
     * @throws com.github.b3kt.domain.exception.AuthenticationException if authentication fails
     */
    LoginResponse login(String username, String password);
    
    /**
     * Get user information from JWT token.
     * 
     * @param jwt the JSON Web Token
     * @return UserInfo with user details
     */
    UserInfo getUserInfo(JsonWebToken jwt);
    
    /**
     * Refresh access token using a valid refresh token.
     * 
     * @param refreshToken the refresh token
     * @return LoginResponse with new tokens
     * @throws com.github.b3kt.domain.exception.AuthenticationException if refresh token is invalid
     */
    LoginResponse refreshToken(String refreshToken);
}

