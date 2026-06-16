package com.github.b3kt.application.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginRequestTest {

    @Test
    @DisplayName("Default constructor")
    void defaultConstructor() {
        LoginRequest r = new LoginRequest();
        assertNull(r.getUsername());
        assertNull(r.getPassword());
    }

    @Test
    @DisplayName("Parameterized constructor")
    void parameterizedConstructor() {
        LoginRequest r = new LoginRequest("admin", "secret");
        assertEquals("admin", r.getUsername());
        assertEquals("secret", r.getPassword());
    }

    @Test
    @DisplayName("Setters")
    void setters() {
        LoginRequest r = new LoginRequest();
        r.setUsername("user");
        r.setPassword("pass");
        assertEquals("user", r.getUsername());
        assertEquals("pass", r.getPassword());
    }
}
