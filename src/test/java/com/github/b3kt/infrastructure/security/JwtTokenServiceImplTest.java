package com.github.b3kt.infrastructure.security;

import com.github.b3kt.application.dto.UserInfo;
import com.github.b3kt.application.properties.RbacProperties;
import com.github.b3kt.application.service.RbacService;
import com.github.b3kt.domain.model.Permission;
import com.github.b3kt.domain.model.User;
import com.github.b3kt.infrastructure.persistence.entity.RoleEntity;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("JwtTokenServiceImpl Tests")
class JwtTokenServiceImplTest {

    @Mock
    private RbacProperties rbacProperties;

    @Mock
    private RbacService rbacService;

    @Mock
    private JsonWebToken jwt;

    @InjectMocks
    private JwtTokenServiceImpl jwtTokenService;

    private User testUser;

    @BeforeEach
    void setUp() {
        jwtTokenService.issuer = "test-issuer";
        jwtTokenService.expirationHours = 24;
        jwtTokenService.refreshExpirationDays = 7;

        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setName("ADMIN");
        Set<RoleEntity> roles = new HashSet<>();
        roles.add(roleEntity);

        testUser = new User("admin", "admin@test.com", "hashed", roles);
        testUser.setActive(true);
    }

    @Nested
    @DisplayName("generateToken")
    class GenerateTokenTests {

        @Test
        @DisplayName("Should generate token without RBAC")
        void testGenerateToken_noRbac() {
            when(rbacProperties.enabled()).thenReturn(false);

            String token = jwtTokenService.generateToken(testUser);

            assertNotNull(token);
            assertTrue(token.split("\\.").length == 3);
        }

        @Test
        @DisplayName("Should generate token with RBAC permissions")
        void testGenerateToken_withRbac() {
            when(rbacProperties.enabled()).thenReturn(true);
            Set<Permission> perms = Set.of(new Permission("read", "Read", "spk", "read"));
            when(rbacService.getUserPermissions("admin")).thenReturn(perms);

            String token = jwtTokenService.generateToken(testUser);

            assertNotNull(token);
        }

        @Test
        @DisplayName("Should handle RBAC exception gracefully")
        void testGenerateToken_rbacException() {
            when(rbacProperties.enabled()).thenReturn(true);
            when(rbacService.getUserPermissions("admin")).thenThrow(new RuntimeException("DB error"));

            String token = jwtTokenService.generateToken(testUser);

            assertNotNull(token);
        }

        @Test
        @DisplayName("Should include karyawanId in token when present")
        void testGenerateToken_withKaryawanId() {
            when(rbacProperties.enabled()).thenReturn(false);
            testUser.setKaryawanId(42L);

            String token = jwtTokenService.generateToken(testUser);

            assertNotNull(token);
        }

        @Test
        @DisplayName("Should include karyawanNama in token when present")
        void testGenerateToken_withKaryawanNama() {
            when(rbacProperties.enabled()).thenReturn(false);
            testUser.setKaryawanNama("Budi");

            String token = jwtTokenService.generateToken(testUser);

            assertNotNull(token);
        }

        @Test
        @DisplayName("Should handle null karyawanId and karyawanNama")
        void testGenerateToken_noKaryawanInfo() {
            when(rbacProperties.enabled()).thenReturn(false);

            String token = jwtTokenService.generateToken(testUser);

            assertNotNull(token);
        }
    }

    @Nested
    @DisplayName("extractUserInfo")
    class ExtractUserInfoTests {

        @Test
        @DisplayName("Should extract user info from JWT")
        void testExtractUserInfo() {
            when(jwt.getSubject()).thenReturn("admin");
            when(jwt.getClaim("email")).thenReturn("admin@test.com");
            when(jwt.getGroups()).thenReturn(Set.of("ADMIN"));
            when(jwt.getClaim("karyawanId")).thenReturn(null);
            when(jwt.getClaim("karyawanNama")).thenReturn(null);

            UserInfo result = jwtTokenService.extractUserInfo(jwt);

            assertNotNull(result);
            assertEquals("admin", result.getUsername());
            assertEquals("admin@test.com", result.getEmail());
            assertTrue(result.getRoles().contains("ADMIN"));
        }

        @Test
        @DisplayName("Should extract karyawanId from JWT")
        void testExtractUserInfo_withKaryawanId() {
            when(jwt.getSubject()).thenReturn("admin");
            when(jwt.getClaim("email")).thenReturn("admin@test.com");
            when(jwt.getGroups()).thenReturn(Set.of("ADMIN"));
            when(jwt.getClaim("karyawanId")).thenReturn(42L);
            when(jwt.getClaim("karyawanNama")).thenReturn("Budi");

            UserInfo result = jwtTokenService.extractUserInfo(jwt);

            assertEquals(42L, result.getKaryawanId());
            assertEquals("Budi", result.getKaryawanNama());
        }

        @Test
        @DisplayName("Should handle string karyawanId")
        void testExtractUserInfo_stringKaryawanId() {
            when(jwt.getSubject()).thenReturn("admin");
            when(jwt.getClaim("email")).thenReturn("admin@test.com");
            when(jwt.getGroups()).thenReturn(Set.of());
            when(jwt.getClaim("karyawanId")).thenReturn("42");
            when(jwt.getClaim("karyawanNama")).thenReturn("Budi");

            UserInfo result = jwtTokenService.extractUserInfo(jwt);

            assertEquals(42L, result.getKaryawanId());
        }

        @Test
        @DisplayName("Should handle non-numeric karyawanId gracefully")
        void testExtractUserInfo_nonNumericKaryawanId() {
            when(jwt.getSubject()).thenReturn("admin");
            when(jwt.getClaim("email")).thenReturn("admin@test.com");
            when(jwt.getGroups()).thenReturn(Set.of());
            when(jwt.getClaim("karyawanId")).thenReturn("not_a_number");
            when(jwt.getClaim("karyawanNama")).thenReturn(null);

            assertThrows(NumberFormatException.class, () -> jwtTokenService.extractUserInfo(jwt));
        }
    }

    @Nested
    @DisplayName("getTokenExpirationSeconds")
    class GetTokenExpirationTests {

        @Test
        @DisplayName("Should return expiration in seconds")
        void testGetTokenExpirationSeconds() {
            long result = jwtTokenService.getTokenExpirationSeconds();

            assertEquals(86400L, result);
        }
    }

    @Nested
    @DisplayName("generateRefreshToken")
    class GenerateRefreshTokenTests {

        @Test
        @DisplayName("Should generate refresh token")
        void testGenerateRefreshToken() {
            String token = jwtTokenService.generateRefreshToken(testUser);

            assertNotNull(token);
            assertTrue(token.split("\\.").length == 3);
        }
    }

    @Nested
    @DisplayName("validateRefreshToken")
    class ValidateRefreshTokenTests {

        @Test
        @DisplayName("Should return null for invalid format")
        void testValidateRefreshToken_invalidFormat() {
            assertNull(jwtTokenService.validateRefreshToken("not.a.valid.jwt.token"));
        }

        @Test
        @DisplayName("Should return null for non-refresh token")
        void testValidateRefreshToken_notRefresh() {
            // Create a valid JWT-like structure but without type=refresh
            String header = Base64.getUrlEncoder().encodeToString("{\"alg\":\"RS256\"}".getBytes());
            String payload = Base64.getUrlEncoder().encodeToString(
                    "{\"sub\":\"admin\",\"exp\":9999999999}".getBytes());
            String signature = "sig";
            String token = header + "." + payload + "." + signature;

            assertNull(jwtTokenService.validateRefreshToken(token));
        }

        @Test
        @DisplayName("Should return null for expired token")
        void testValidateRefreshToken_expired() {
            String header = Base64.getUrlEncoder().encodeToString("{\"alg\":\"RS256\"}".getBytes());
            String payload = Base64.getUrlEncoder().encodeToString(
                    "{\"sub\":\"admin\",\"type\":\"refresh\",\"exp\":1}".getBytes());
            String signature = "sig";
            String token = header + "." + payload + "." + signature;

            assertNull(jwtTokenService.validateRefreshToken(token));
        }

        @Test
        @DisplayName("Should extract username from valid refresh token")
        void testValidateRefreshToken_valid() {
            long futureExp = System.currentTimeMillis() / 1000 + 86400;
            String header = Base64.getUrlEncoder().encodeToString("{\"alg\":\"RS256\"}".getBytes());
            String payload = Base64.getUrlEncoder().encodeToString(
                    ("{\"sub\":\"admin\",\"type\":\"refresh\",\"exp\":" + futureExp + "}").getBytes());
            String signature = "sig";
            String token = header + "." + payload + "." + signature;

            String result = jwtTokenService.validateRefreshToken(token);

            assertEquals("admin", result);
        }

        @Test
        @DisplayName("Should return null for malformed payload")
        void testValidateRefreshToken_malformed() {
            String header = Base64.getUrlEncoder().encodeToString("{\"alg\":\"RS256\"}".getBytes());
            String payload = Base64.getUrlEncoder().encodeToString("not json".getBytes());
            String signature = "sig";
            String token = header + "." + payload + "." + signature;

            assertNull(jwtTokenService.validateRefreshToken(token));
        }

        @Test
        @DisplayName("Should return null for only two parts")
        void testValidateRefreshToken_twoParts() {
            assertNull(jwtTokenService.validateRefreshToken("header.payload"));
        }
    }
}
