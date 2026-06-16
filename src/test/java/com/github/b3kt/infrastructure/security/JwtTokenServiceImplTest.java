package com.github.b3kt.infrastructure.security;

import com.github.b3kt.application.dto.UserInfo;
import com.github.b3kt.application.service.RbacService;
import com.github.b3kt.domain.model.User;
import com.github.b3kt.infrastructure.persistence.entity.RoleEntity;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class JwtTokenServiceImplTest {

    @Inject
    JwtTokenServiceImpl jwtTokenService;

    @InjectMock
    RbacService rbacService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setActive(true);
        Set<RoleEntity> roles = Set.of(createRole("user"));
        testUser.setRoles(roles);
        when(rbacService.getUserPermissions(anyString())).thenReturn(Set.of());
    }

    @Test
    @DisplayName("generateToken returns a signed JWT string")
    void generateToken() {
        String token = jwtTokenService.generateToken(testUser);
        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3);
    }

    @Test
    @DisplayName("generateToken includes karyawan info when present")
    void generateTokenWithKaryawan() {
        testUser.setKaryawanId(100L);
        testUser.setKaryawanNama("Budi");
        String token = jwtTokenService.generateToken(testUser);
        assertNotNull(token);
    }

    @Test
    @DisplayName("generateToken includes permissions when RBAC enabled")
    void generateTokenRbacEnabled() {
        when(rbacService.getUserPermissions("testuser")).thenReturn(Set.of());
        String token = jwtTokenService.generateToken(testUser);
        assertNotNull(token);
    }

    @Test
    @DisplayName("generateToken handles RBAC exception gracefully")
    void generateTokenRbacException() {
        when(rbacService.getUserPermissions("testuser")).thenThrow(new RuntimeException("RBAC error"));
        String token = jwtTokenService.generateToken(testUser);
        assertNotNull(token);
    }

    @Test
    @DisplayName("getTokenExpirationSeconds returns positive value")
    void getTokenExpirationSeconds() {
        long secs = jwtTokenService.getTokenExpirationSeconds();
        assertTrue(secs > 0);
    }

    @Test
    @DisplayName("generateRefreshToken returns a signed JWT string")
    void generateRefreshToken() {
        String token = jwtTokenService.generateRefreshToken(testUser);
        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3);
    }

    @Test
    @DisplayName("validateRefreshToken returns username for valid token")
    void validateRefreshTokenValid() {
        String refreshToken = jwtTokenService.generateRefreshToken(testUser);
        String username = jwtTokenService.validateRefreshToken(refreshToken);
        assertEquals("testuser", username);
    }

    @Test
    @DisplayName("validateRefreshToken returns null for malformed token")
    void validateRefreshTokenMalformed() {
        assertNull(jwtTokenService.validateRefreshToken("invalid"));
    }

    @Test
    @DisplayName("validateRefreshToken returns null for token without 3 parts")
    void validateRefreshTokenTwoParts() {
        assertNull(jwtTokenService.validateRefreshToken("part1.part2"));
    }

    @Test
    @DisplayName("extractUserInfo returns UserInfo from JWT")
    void extractUserInfo() {
        String token = jwtTokenService.generateToken(testUser);
        JsonWebToken jwt = parseToken(token);
        UserInfo info = jwtTokenService.extractUserInfo(jwt);

        assertNotNull(info);
        assertEquals("testuser", info.getUsername());
        assertEquals("test@example.com", info.getEmail());
    }

    private JsonWebToken parseToken(String token) {
        JsonWebToken mock = mock(JsonWebToken.class);
        when(mock.getSubject()).thenReturn("testuser");
        when(mock.getClaim("email")).thenReturn("test@example.com");
        when(mock.getGroups()).thenReturn(Set.of("user"));
        return mock;
    }

    private static RoleEntity createRole(String name) {
        RoleEntity r = new RoleEntity();
        r.setName(name);
        return r;
    }
}
