package com.github.b3kt.application.service.impl;

import com.github.b3kt.application.dto.LoginResponse;
import com.github.b3kt.application.dto.UserInfo;
import com.github.b3kt.application.service.RefreshTokenService;
import com.github.b3kt.domain.exception.AuthenticationException;
import com.github.b3kt.domain.model.User;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbKaryawanEntity;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbKaryawanRepository;
import com.github.b3kt.infrastructure.repository.UserRepository;
import com.github.b3kt.infrastructure.security.JwtTokenService;
import com.github.b3kt.infrastructure.security.PasswordEncoder;
import com.github.b3kt.infrastructure.security.RefreshTokenClaims;
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

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AuthServiceImpl Tests")
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TbKaryawanRepository tbKaryawanRepository;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private JsonWebToken jwt;

    private final RefreshTokenClaims refreshClaims = new RefreshTokenClaims("admin", "token-id");

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("admin", "admin@test.com", "hashed_password", Set.of());
        testUser.setActive(true);
    }

    @Nested
    @DisplayName("login")
    class LoginTests {

        @Test
        @DisplayName("Should login successfully")
        void testLogin_success() {
            when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("password", "hashed_password")).thenReturn(true);
            when(jwtTokenService.generateToken(any(User.class))).thenReturn("token123");
            when(refreshTokenService.issue(any(User.class))).thenReturn("refresh123");
            when(jwtTokenService.getTokenExpirationSeconds()).thenReturn(86400L);

            LoginResponse result = authService.login("admin", "password");

            assertNotNull(result);
            assertEquals("token123", result.getToken());
            assertEquals("refresh123", result.getRefreshToken());
            assertEquals("admin", result.getUsername());
            assertEquals(86400L, result.getExpiresIn());
            assertFalse(result.isMustChangePassword());
        }

        @Test
        @DisplayName("Should flag a login with a temporary password")
        void testLogin_mustChangePassword() {
            testUser.setMustChangePassword(true);
            when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("password", "hashed_password")).thenReturn(true);

            LoginResponse result = authService.login("admin", "password");

            assertTrue(result.isMustChangePassword());
        }

        @Test
        @DisplayName("Should throw when user not found")
        void testLogin_userNotFound() {
            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            AuthenticationException ex = assertThrows(AuthenticationException.class,
                    () -> authService.login("nonexistent", "password"));

            assertEquals("Invalid username or password", ex.getMessage());
        }

        @Test
        @DisplayName("Should throw when user inactive and the password is correct")
        void testLogin_userInactive() {
            testUser.setActive(false);
            when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("password", "hashed_password")).thenReturn(true);

            AuthenticationException ex = assertThrows(AuthenticationException.class,
                    () -> authService.login("admin", "password"));

            assertEquals("User account is not active", ex.getMessage());
        }

        @Test
        @DisplayName("Should not reveal that an account is inactive to a wrong password")
        void testLogin_userInactiveWrongPassword() {
            testUser.setActive(false);
            when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("wrong", "hashed_password")).thenReturn(false);

            AuthenticationException ex = assertThrows(AuthenticationException.class,
                    () -> authService.login("admin", "wrong"));

            assertEquals("Invalid username or password", ex.getMessage());
        }

        @Test
        @DisplayName("Should still check a password for unknown users, so they are not faster to reject")
        void testLogin_unknownUserChecksDummyHash() {
            when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$dummy");

            assertThrows(AuthenticationException.class, () -> authService.login("ghost", "password"));

            verify(passwordEncoder).matches("password", "$2a$10$dummy");
        }

        @Test
        @DisplayName("Should throw when user has null username")
        void testLogin_nullUsername() {
            testUser.setUsername(null);
            when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("password", "hashed_password")).thenReturn(true);

            AuthenticationException ex = assertThrows(AuthenticationException.class,
                    () -> authService.login("admin", "password"));

            assertEquals("User account is not active", ex.getMessage());
        }

        @Test
        @DisplayName("Should throw when password doesn't match")
        void testLogin_wrongPassword() {
            when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("wrong", "hashed_password")).thenReturn(false);

            AuthenticationException ex = assertThrows(AuthenticationException.class,
                    () -> authService.login("admin", "wrong"));

            assertEquals("Invalid username or password", ex.getMessage());
        }

        @Test
        @DisplayName("Should enrich user with karyawan info when found")
        void testLogin_withKaryawan() {
            when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("password", "hashed_password")).thenReturn(true);

            TbKaryawanEntity karyawan = new TbKaryawanEntity();
            karyawan.setId(42L);
            karyawan.setNamaKaryawan("Budi");
            when(tbKaryawanRepository.findByUsername("admin")).thenReturn(Optional.of(karyawan));
            when(jwtTokenService.generateToken(any(User.class))).thenReturn("token123");
            when(refreshTokenService.issue(any(User.class))).thenReturn("refresh123");
            when(jwtTokenService.getTokenExpirationSeconds()).thenReturn(86400L);

            LoginResponse result = authService.login("admin", "password");

            assertNotNull(result);
            assertEquals("admin", result.getUsername());
        }

        @Test
        @DisplayName("Should handle missing karyawan gracefully")
        void testLogin_withoutKaryawan() {
            when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("password", "hashed_password")).thenReturn(true);
            when(tbKaryawanRepository.findByUsername("admin")).thenReturn(Optional.empty());
            when(jwtTokenService.generateToken(any(User.class))).thenReturn("token123");
            when(refreshTokenService.issue(any(User.class))).thenReturn("refresh123");
            when(jwtTokenService.getTokenExpirationSeconds()).thenReturn(86400L);

            LoginResponse result = authService.login("admin", "password");

            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("refreshToken")
    class RefreshTokenTests {

        @Test
        @DisplayName("Should refresh token successfully")
        void testRefreshToken_success() {
            when(jwtTokenService.validateRefreshToken("refresh_token")).thenReturn(refreshClaims);
            when(refreshTokenService.consume(refreshClaims)).thenReturn("family-1");
            when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));
            when(jwtTokenService.generateToken(any(User.class))).thenReturn("new_token");
            when(refreshTokenService.issue(any(User.class), eq("family-1"))).thenReturn("new_refresh");
            when(jwtTokenService.getTokenExpirationSeconds()).thenReturn(86400L);

            LoginResponse result = authService.refreshToken("refresh_token");

            assertNotNull(result);
            assertEquals("new_token", result.getToken());
            assertEquals("new_refresh", result.getRefreshToken());
            verify(refreshTokenService).consume(refreshClaims);
        }

        @Test
        @DisplayName("Should reject a rotated or revoked refresh token")
        void testRefreshToken_consumeRejected() {
            when(jwtTokenService.validateRefreshToken("refresh_token")).thenReturn(refreshClaims);
            when(refreshTokenService.consume(refreshClaims))
                    .thenThrow(new AuthenticationException("Invalid or expired refresh token"));

            assertThrows(AuthenticationException.class, () -> authService.refreshToken("refresh_token"));
            verify(jwtTokenService, never()).generateToken(any(User.class));
        }

        @Test
        @DisplayName("Should throw when refresh token invalid")
        void testRefreshToken_invalid() {
            when(jwtTokenService.validateRefreshToken("invalid")).thenReturn(null);

            AuthenticationException ex = assertThrows(AuthenticationException.class,
                    () -> authService.refreshToken("invalid"));

            assertEquals("Invalid or expired refresh token", ex.getMessage());
            verify(refreshTokenService, never()).consume(any());
        }

        @Test
        @DisplayName("Should throw when user not found after refresh")
        void testRefreshToken_userNotFound() {
            RefreshTokenClaims claims = new RefreshTokenClaims("nonexistent", "token-id");
            when(jwtTokenService.validateRefreshToken("refresh_token")).thenReturn(claims);
            when(refreshTokenService.consume(claims)).thenReturn("family-1");
            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            AuthenticationException ex = assertThrows(AuthenticationException.class,
                    () -> authService.refreshToken("refresh_token"));

            assertEquals("User not found", ex.getMessage());
        }

        @Test
        @DisplayName("Should throw when user inactive during refresh")
        void testRefreshToken_userInactive() {
            testUser.setActive(false);
            when(jwtTokenService.validateRefreshToken("refresh_token")).thenReturn(refreshClaims);
            when(refreshTokenService.consume(refreshClaims)).thenReturn("family-1");
            when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));

            AuthenticationException ex = assertThrows(AuthenticationException.class,
                    () -> authService.refreshToken("refresh_token"));

            assertEquals("User account is not active", ex.getMessage());
            verify(refreshTokenService).revokeAllForUser("admin");
        }

        @Test
        @DisplayName("Should enrich with karyawan info during refresh")
        void testRefreshToken_withKaryawan() {
            when(jwtTokenService.validateRefreshToken("refresh_token")).thenReturn(refreshClaims);
            when(refreshTokenService.consume(refreshClaims)).thenReturn("family-1");
            when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));

            TbKaryawanEntity karyawan = new TbKaryawanEntity();
            karyawan.setId(42L);
            karyawan.setNamaKaryawan("Budi");
            when(tbKaryawanRepository.findByUsername("admin")).thenReturn(Optional.of(karyawan));
            when(jwtTokenService.generateToken(any(User.class))).thenReturn("new_token");
            when(refreshTokenService.issue(any(User.class), eq("family-1"))).thenReturn("new_refresh");
            when(jwtTokenService.getTokenExpirationSeconds()).thenReturn(86400L);

            LoginResponse result = authService.refreshToken("refresh_token");

            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("changePassword")
    class ChangePasswordTests {

        @BeforeEach
        void stubTokens() {
            when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));
            when(jwtTokenService.generateToken(any(User.class))).thenReturn("new_token");
            when(refreshTokenService.issue(any(User.class))).thenReturn("new_refresh");
        }

        @Test
        @DisplayName("Should store the new hash, clear the flag, end other sessions and return new tokens")
        void testChangePassword_success() {
            testUser.setMustChangePassword(true);
            when(passwordEncoder.matches("old-password", "hashed_password")).thenReturn(true);
            when(passwordEncoder.encode("New-password-1")).thenReturn("$2a$new");

            LoginResponse result = authService.changePassword("admin", "old-password", "New-password-1");

            verify(userRepository).updatePassword("admin", "$2a$new", false);
            verify(refreshTokenService).revokeAllForUser("admin");
            assertEquals("new_token", result.getToken());
            assertEquals("new_refresh", result.getRefreshToken());
            assertFalse(result.isMustChangePassword());
        }

        @Test
        @DisplayName("Should reject a wrong current password")
        void testChangePassword_wrongCurrent() {
            when(passwordEncoder.matches("wrong", "hashed_password")).thenReturn(false);

            assertThrows(AuthenticationException.class,
                    () -> authService.changePassword("admin", "wrong", "New-password-1"));
            verify(userRepository, never()).updatePassword(anyString(), anyString(), anyBoolean());
        }

        @Test
        @DisplayName("Should reject a weak or unchanged new password")
        void testChangePassword_policy() {
            when(passwordEncoder.matches("old-password", "hashed_password")).thenReturn(true);

            assertThrows(IllegalArgumentException.class,
                    () -> authService.changePassword("admin", "old-password", "short"));
            assertThrows(IllegalArgumentException.class,
                    () -> authService.changePassword("admin", "old-password", "old-password"));
            verify(userRepository, never()).updatePassword(anyString(), anyString(), anyBoolean());
        }

        @Test
        @DisplayName("Should reject an inactive user")
        void testChangePassword_inactive() {
            testUser.setActive(false);

            assertThrows(AuthenticationException.class,
                    () -> authService.changePassword("admin", "old-password", "New-password-1"));
        }
    }

    @Nested
    @DisplayName("logout")
    class LogoutTests {

        @Test
        @DisplayName("Should revoke the session of the given refresh token")
        void testLogout_withRefreshToken() {
            when(jwtTokenService.validateRefreshToken("refresh_token")).thenReturn(refreshClaims);

            authService.logout("admin", "refresh_token");

            verify(refreshTokenService).revokeSession(refreshClaims);
            verify(refreshTokenService, never()).revokeAllForUser(anyString());
        }

        @Test
        @DisplayName("Should revoke all sessions when no refresh token is given")
        void testLogout_withoutRefreshToken() {
            authService.logout("admin", null);

            verify(refreshTokenService).revokeAllForUser("admin");
        }

        @Test
        @DisplayName("Should not revoke another user's session")
        void testLogout_otherUsersToken() {
            when(jwtTokenService.validateRefreshToken("refresh_token"))
                    .thenReturn(new RefreshTokenClaims("someone-else", "token-id"));

            authService.logout("admin", "refresh_token");

            verify(refreshTokenService, never()).revokeSession(any());
            verify(refreshTokenService, never()).revokeAllForUser(anyString());
        }
    }

    @Nested
    @DisplayName("getUserInfo")
    class GetUserInfoTests {

        @Test
        @DisplayName("Should extract user info from JWT")
        void testGetUserInfo() {
            UserInfo expected = new UserInfo("admin", "admin@test.com", Set.of("ADMIN"), null, null);
            when(jwtTokenService.extractUserInfo(jwt)).thenReturn(expected);

            UserInfo result = authService.getUserInfo(jwt);

            assertNotNull(result);
            assertEquals("admin", result.getUsername());
            verify(jwtTokenService).extractUserInfo(jwt);
        }
    }
}
