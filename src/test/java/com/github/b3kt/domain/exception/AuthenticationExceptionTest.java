package com.github.b3kt.domain.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthenticationExceptionTest {

    @Test
    @DisplayName("Constructor with message")
    void constructorWithMessage() {
        AuthenticationException e = new AuthenticationException("Invalid credentials");
        assertEquals("Invalid credentials", e.getMessage());
    }

    @Test
    @DisplayName("Constructor with message and cause")
    void constructorWithMessageAndCause() {
        Throwable cause = new RuntimeException("root cause");
        AuthenticationException e = new AuthenticationException("Invalid credentials", cause);
        assertEquals("Invalid credentials", e.getMessage());
        assertEquals(cause, e.getCause());
    }
}
