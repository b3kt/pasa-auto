package com.github.b3kt.domain.model;

import com.github.b3kt.infrastructure.persistence.entity.RoleEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    @DisplayName("Default constructor creates inactive user")
    void defaultConstructor() {
        User user = new User();
        assertFalse(user.isActive());
        assertNull(user.getUsername());
    }

    @Test
    @DisplayName("Parameterized constructor sets fields and active=true")
    void parameterizedConstructor() {
        Set<RoleEntity> roles = Set.of(new RoleEntity("admin", "Admin role"));
        User user = new User("testuser", "test@example.com", "hash", roles);
        assertEquals("testuser", user.getUsername());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("hash", user.getPasswordHash());
        assertEquals(roles, user.getRoles());
        assertTrue(user.isActive());
    }

    @Test
    @DisplayName("Full constructor with karyawan info")
    void fullConstructor() {
        Set<RoleEntity> roles = Set.of();
        User user = new User("u", "e@x.com", "hash", roles, 100L, "Budi");
        assertEquals(100L, user.getKaryawanId());
        assertEquals("Budi", user.getKaryawanNama());
        assertTrue(user.isActive());
    }

    @Test
    @DisplayName("canAuthenticate returns true when active with non-empty username")
    void canAuthenticateActiveWithUsername() {
        User user = new User();
        user.setActive(true);
        user.setUsername("user");
        assertTrue(user.canAuthenticate());
    }

    @Test
    @DisplayName("canAuthenticate returns false when inactive")
    void canAuthenticateInactive() {
        User user = new User();
        user.setActive(false);
        user.setUsername("user");
        assertFalse(user.canAuthenticate());
    }

    @Test
    @DisplayName("canAuthenticate returns false when username is null")
    void canAuthenticateNullUsername() {
        User user = new User();
        user.setActive(true);
        user.setUsername(null);
        assertFalse(user.canAuthenticate());
    }

    @Test
    @DisplayName("canAuthenticate returns false when username is empty")
    void canAuthenticateEmptyUsername() {
        User user = new User();
        user.setActive(true);
        user.setUsername("");
        assertFalse(user.canAuthenticate());
    }

    @Test
    @DisplayName("Setters and getters work correctly")
    void settersAndGetters() {
        User user = new User();
        user.setUsername("u");
        user.setEmail("e@x.com");
        user.setPasswordHash("hash");
        user.setActive(true);
        user.setKaryawanId(1L);
        user.setKaryawanNama("Budi");
        Set<RoleEntity> roles = Set.of(new RoleEntity("admin", ""));
        user.setRoles(roles);

        assertEquals("u", user.getUsername());
        assertEquals("e@x.com", user.getEmail());
        assertEquals("hash", user.getPasswordHash());
        assertTrue(user.isActive());
        assertEquals(1L, user.getKaryawanId());
        assertEquals("Budi", user.getKaryawanNama());
        assertEquals(roles, user.getRoles());
    }
}
