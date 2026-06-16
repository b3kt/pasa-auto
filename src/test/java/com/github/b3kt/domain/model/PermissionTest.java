package com.github.b3kt.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PermissionTest {

    @Test
    @DisplayName("Default constructor sets active=true")
    void defaultConstructor() {
        Permission p = new Permission();
        assertTrue(p.isActive());
    }

    @Test
    @DisplayName("Parameterized constructor sets all fields")
    void parameterizedConstructor() {
        Permission p = new Permission("read:users", "Read users", "users", "read");
        assertEquals("read:users", p.getName());
        assertEquals("Read users", p.getDescription());
        assertEquals("users", p.getResource());
        assertEquals("read", p.getAction());
        assertTrue(p.isActive());
    }

    @Test
    @DisplayName("Setters and getters")
    void settersAndGetters() {
        Permission p = new Permission();
        p.setId(1L);
        p.setName("write:users");
        p.setDescription("Write users");
        p.setResource("users");
        p.setAction("write");
        p.setActive(false);

        assertEquals(1L, p.getId());
        assertEquals("write:users", p.getName());
        assertEquals("Write users", p.getDescription());
        assertEquals("users", p.getResource());
        assertEquals("write", p.getAction());
        assertFalse(p.isActive());
    }

    @Test
    @DisplayName("isValid returns true for valid permission")
    void isValidValid() {
        Permission p = new Permission("read:users", "desc", "users", "read");
        assertTrue(p.isValid());
    }

    @Test
    @DisplayName("isValid returns false when name is null")
    void isValidNullName() {
        Permission p = new Permission(null, "desc", "users", "read");
        assertFalse(p.isValid());
    }

    @Test
    @DisplayName("isValid returns false when name is empty")
    void isValidEmptyName() {
        Permission p = new Permission("", "desc", "users", "read");
        assertFalse(p.isValid());
    }

    @Test
    @DisplayName("isValid returns false when resource is null")
    void isValidNullResource() {
        Permission p = new Permission("read:users", "desc", null, "read");
        assertFalse(p.isValid());
    }

    @Test
    @DisplayName("isValid returns false when action is null")
    void isValidNullAction() {
        Permission p = new Permission("read:users", "desc", "users", null);
        assertFalse(p.isValid());
    }

    @Test
    @DisplayName("isValid returns false when inactive")
    void isValidInactive() {
        Permission p = new Permission("read:users", "desc", "users", "read");
        p.setActive(false);
        assertFalse(p.isValid());
    }
}
