package com.github.b3kt.infrastructure.security;

/**
 * Interface for password encoding/verification.
 */
public interface PasswordEncoder {
    
    /**
     * Encode a raw password.
     * 
     * @param rawPassword the raw password
     * @return the encoded password
     */
    String encode(String rawPassword);
    
    /**
     * Verify if a raw password matches an encoded password.
     * 
     * @param rawPassword the raw password
     * @param encodedPassword the encoded password
     * @return true if passwords match
     */
    boolean matches(String rawPassword, String encodedPassword);
}

