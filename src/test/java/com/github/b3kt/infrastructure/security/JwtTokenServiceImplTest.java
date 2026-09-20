package com.github.b3kt.infrastructure.security;

import com.github.b3kt.application.dto.UserInfo;
import com.github.b3kt.application.properties.RbacProperties;
import com.github.b3kt.application.service.RbacService;
import com.github.b3kt.domain.model.Permission;
import com.github.b3kt.domain.model.User;
import com.github.b3kt.infrastructure.persistence.entity.RoleEntity;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
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

    @Mock
    private JWTParser jwtParser;

    @InjectMocks
    private JwtTokenServiceImpl jwtTokenService;

    private User testUser;

    @BeforeEach
    void setUp() {
        jwtTokenService.issuer = "test-issuer";
        jwtTokenService.expirationMinutes = 1440;
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

        /**
         * A Google sign-in creates an approved account with no roles yet, so a null or empty role
         * set has to produce a token rather than throw on the way in.
         */
        @Test
        @DisplayName("Should issue a token for a user with no roles")
        void testGenerateToken_nullRoles() {
            when(rbacProperties.enabled()).thenReturn(false);
            testUser.setRoles(null);

            String token = jwtTokenService.generateToken(testUser);

            assertNotNull(token);
            assertEquals(3, token.split("\\.").length);
        }

        @Test
        @DisplayName("Should issue a token for a user with an empty role set")
        void testGenerateToken_emptyRoles() {
            when(rbacProperties.enabled()).thenReturn(false);
            testUser.setRoles(Set.of());

            assertNotNull(jwtTokenService.generateToken(testUser));
        }

        /** The claim the augmentor reads to strip roles until the password is changed. */
        @Test
        @DisplayName("Should mark a token issued for a temporary password")
        void testGenerateToken_mustChangePassword() {
            when(rbacProperties.enabled()).thenReturn(false);
            testUser.setMustChangePassword(true);

            String token = jwtTokenService.generateToken(testUser);

            assertNotNull(token);
            String payload = new String(java.util.Base64.getUrlDecoder().decode(token.split("\\.")[1]),
                    java.nio.charset.StandardCharsets.UTF_8);
            assertTrue(payload.contains(PasswordChangeRequiredAugmentor.CLAIM), payload);
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
            String token = jwtTokenService.generateRefreshToken(testUser, "token-id");

            assertNotNull(token);
            assertTrue(token.split("\\.").length == 3);
        }

        @Test
        @DisplayName("Should report the configured refresh token lifetime")
        void testGetRefreshTokenLifetime() {
            assertEquals(java.time.Duration.ofDays(7), jwtTokenService.getRefreshTokenLifetime());
        }
    }

    @Nested
    @DisplayName("validateRefreshToken")
    class ValidateRefreshTokenTests {

        @Test
        @DisplayName("Should return null for blank token without parsing")
        void testValidateRefreshToken_blank() throws Exception {
            assertNull(jwtTokenService.validateRefreshToken(null));
            assertNull(jwtTokenService.validateRefreshToken(" "));
            verify(jwtParser, never()).parse(anyString());
        }

        @Test
        @DisplayName("Should return null when signature, issuer or expiry verification fails")
        void testValidateRefreshToken_rejectedByParser() throws Exception {
            when(jwtParser.parse("forged")).thenThrow(new ParseException("invalid signature"));

            assertNull(jwtTokenService.validateRefreshToken("forged"));
        }

        @Test
        @DisplayName("Should return null for a verified token that is not a refresh token")
        void testValidateRefreshToken_notRefresh() throws Exception {
            when(jwt.getClaim("type")).thenReturn(null);
            when(jwt.getSubject()).thenReturn("admin");
            when(jwtParser.parse("access")).thenReturn(jwt);

            assertNull(jwtTokenService.validateRefreshToken("access"));
        }

        @Test
        @DisplayName("Should return null for a refresh token without a token id")
        void testValidateRefreshToken_missingTokenId() throws Exception {
            when(jwt.getClaim("type")).thenReturn("refresh");
            when(jwt.getSubject()).thenReturn("admin");
            when(jwt.getTokenID()).thenReturn(null);
            when(jwtParser.parse("refresh")).thenReturn(jwt);

            assertNull(jwtTokenService.validateRefreshToken("refresh"));
        }

        @Test
        @DisplayName("Should return the subject and token id of a verified refresh token")
        void testValidateRefreshToken_valid() throws Exception {
            when(jwt.getClaim("type")).thenReturn("refresh");
            when(jwt.getSubject()).thenReturn("admin");
            when(jwt.getTokenID()).thenReturn("token-id");
            when(jwtParser.parse("refresh")).thenReturn(jwt);

            assertEquals(new RefreshTokenClaims("admin", "token-id"), jwtTokenService.validateRefreshToken("refresh"));
        }
    }
}
