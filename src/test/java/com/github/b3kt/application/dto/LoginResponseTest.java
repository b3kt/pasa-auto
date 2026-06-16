package com.github.b3kt.application.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginResponseTest {

    @Test
    @DisplayName("Default constructor")
    void defaultConstructor() {
        LoginResponse r = new LoginResponse();
        assertNull(r.getToken());
        assertNull(r.getUsername());
    }

    @Test
    @DisplayName("Constructor without refreshToken")
    void constructorWithoutRefresh() {
        LoginResponse r = new LoginResponse("t", "u", "e@x.com", 3600L);
        assertEquals("t", r.getToken());
        assertNull(r.getRefreshToken());
        assertEquals("u", r.getUsername());
        assertEquals("e@x.com", r.getEmail());
        assertEquals(3600L, r.getExpiresIn());
    }

    @Test
    @DisplayName("Constructor with refreshToken")
    void constructorWithRefresh() {
        LoginResponse r = new LoginResponse("t", "rt", "u", "e@x.com", 3600L);
        assertEquals("t", r.getToken());
        assertEquals("rt", r.getRefreshToken());
        assertEquals("u", r.getUsername());
        assertEquals("e@x.com", r.getEmail());
        assertEquals(3600L, r.getExpiresIn());
    }

    @Test
    @DisplayName("Setters")
    void setters() {
        LoginResponse r = new LoginResponse();
        r.setToken("t");
        r.setRefreshToken("rt");
        r.setUsername("u");
        r.setEmail("e@x.com");
        r.setExpiresIn(7200L);
        assertEquals("t", r.getToken());
        assertEquals("rt", r.getRefreshToken());
        assertEquals("u", r.getUsername());
        assertEquals("e@x.com", r.getEmail());
        assertEquals(7200L, r.getExpiresIn());
    }
}
