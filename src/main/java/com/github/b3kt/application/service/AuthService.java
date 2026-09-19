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
    
    /**
     * End a session by revoking its refresh token family.
     * 
     * @param username the authenticated user
     * @param refreshToken the session's refresh token; when null, all of the user's sessions are revoked
     */
    void logout(String username, String refreshToken);
    
    /**
     * Change the password of an authenticated user. Ends all of the user's other sessions.
     * 
     * @param username the authenticated user
     * @param currentPassword the password being replaced
     * @param newPassword the new password
     * @return a fresh token pair (without the password-change requirement)
     * @throws com.github.b3kt.domain.exception.AuthenticationException if the current password is wrong
     * @throws IllegalArgumentException if the new password violates the password policy
     */
    LoginResponse changePassword(String username, String currentPassword, String newPassword);
}
