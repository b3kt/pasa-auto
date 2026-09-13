package com.github.b3kt.application.mapper;

import com.github.b3kt.application.dto.UserInfo;
import com.github.b3kt.domain.model.User;
import com.github.b3kt.infrastructure.persistence.entity.RoleEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserMapperTest {

    @Test
    @DisplayName("toUserInfo maps all fields correctly")
    void toUserInfo() {
        Set<RoleEntity> roles = Set.of(createRole("admin"), createRole("user"));
        User user = new User("testuser", "test@example.com", "hash", roles, 1L, "Budi");

        UserInfo info = UserMapper.toUserInfo(user);

        assertNotNull(info);
        assertEquals("testuser", info.getUsername());
        assertEquals("test@example.com", info.getEmail());
        assertEquals(Set.of("admin", "user"), info.getRoles());
        assertEquals(1L, info.getKaryawanId());
        assertEquals("Budi", info.getKaryawanNama());
    }

    @Test
    @DisplayName("toUserInfo returns null when user is null")
    void toUserInfoNull() {
        assertNull(UserMapper.toUserInfo(null));
    }

    @Test
    @DisplayName("toUserInfo returns empty roles when roles is null")
    void toUserInfoNullRoles() {
        User user = new User("testuser", "e@x.com", "hash", null);
        UserInfo info = UserMapper.toUserInfo(user);
        assertNotNull(info);
        assertTrue(info.getRoles().isEmpty());
    }

    @Test
    @DisplayName("toUserInfo without karyawan info")
    void toUserInfoNoKaryawan() {
        User user = new User("testuser", "e@x.com", "hash", Set.of());
        UserInfo info = UserMapper.toUserInfo(user);
        assertNull(info.getKaryawanId());
        assertNull(info.getKaryawanNama());
    }

    private static RoleEntity createRole(String name) {
        RoleEntity r = new RoleEntity();
        r.setName(name);
        return r;
    }
}
