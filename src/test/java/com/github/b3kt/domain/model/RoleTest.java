package com.github.b3kt.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RoleTest {

    @Test
    @DisplayName("Default constructor initializes empty permissions and active=true")
    void defaultConstructor() {
        Role role = new Role();
        assertTrue(role.getPermissions().isEmpty());
        assertTrue(role.isActive());
    }

    @Test
    @DisplayName("Parameterized constructor sets name and description")
    void parameterizedConstructor() {
        Role role = new Role("admin", "Administrator role");
        assertEquals("admin", role.getName());
        assertEquals("Administrator role", role.getDescription());
        assertTrue(role.isActive());
    }

    @Test
    @DisplayName("Setters and getters")
    void settersAndGetters() {
        Role role = new Role();
        role.setId(1L);
        role.setName("editor");
        role.setDescription("Editor role");
        role.setActive(false);

        assertEquals(1L, role.getId());
        assertEquals("editor", role.getName());
        assertEquals("Editor role", role.getDescription());
        assertFalse(role.isActive());
    }

    @Test
    @DisplayName("setPermissions with null creates empty set")
    void setPermissionsNull() {
        Role role = new Role();
        role.setPermissions(null);
        assertTrue(role.getPermissions().isEmpty());
    }

    @Test
    @DisplayName("hasPermission returns true when permission exists and active")
    void hasPermissionActive() {
        Role role = new Role();
        Permission perm = new Permission("read", "Read access", "users", "read");
        perm.setActive(true);
        role.addPermission(perm);

        assertTrue(role.hasPermission("read"));
    }

    @Test
    @DisplayName("hasPermission returns false when permission is inactive")
    void hasPermissionInactive() {
        Role role = new Role();
        Permission perm = new Permission("read", "Read access", "users", "read");
        perm.setActive(false);
        role.addPermission(perm);

        assertFalse(role.hasPermission("read"));
    }

    @Test
    @DisplayName("hasPermission returns false when permission does not exist")
    void hasPermissionNotFound() {
        Role role = new Role();
        assertFalse(role.hasPermission("nonexistent"));
    }

    @Test
    @DisplayName("addPermission with null does nothing")
    void addPermissionNull() {
        Role role = new Role();
        role.addPermission(null);
        assertTrue(role.getPermissions().isEmpty());
    }

    @Test
    @DisplayName("addPermission adds permission to set")
    void addPermission() {
        Role role = new Role();
        Permission perm = new Permission("write", "Write access", "users", "write");
        role.addPermission(perm);
        assertEquals(1, role.getPermissions().size());
        assertTrue(role.getPermissions().contains(perm));
    }

    @Test
    @DisplayName("removePermission with null does nothing")
    void removePermissionNull() {
        Role role = new Role();
        role.removePermission(null);
        assertTrue(role.getPermissions().isEmpty());
    }

    @Test
    @DisplayName("removePermission removes permission from set")
    void removePermission() {
        Role role = new Role();
        Permission perm = new Permission("delete", "Delete access", "users", "delete");
        role.addPermission(perm);
        role.removePermission(perm);
        assertTrue(role.getPermissions().isEmpty());
    }
}
