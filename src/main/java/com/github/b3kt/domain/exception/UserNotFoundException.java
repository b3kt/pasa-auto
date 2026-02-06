package com.github.b3kt.domain.exception;

/**
 * Domain exception when user is not found.
 */
public class UserNotFoundException extends RuntimeException {
    
    public UserNotFoundException(String message) {
        super(message);
    }
}

