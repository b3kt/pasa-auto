package com.github.b3kt.domain.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserNotFoundExceptionTest {

    @Test
    @DisplayName("Constructor with message")
    void constructorWithMessage() {
        UserNotFoundException e = new UserNotFoundException("User not found: admin");
        assertEquals("User not found: admin", e.getMessage());
    }
}
