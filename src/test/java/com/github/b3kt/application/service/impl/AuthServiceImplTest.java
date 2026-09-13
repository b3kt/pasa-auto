package com.github.b3kt.application.service.impl;

import com.github.b3kt.application.dto.LoginResponse;
import com.github.b3kt.application.dto.UserInfo;
import com.github.b3kt.domain.exception.AuthenticationException;
import com.github.b3kt.domain.model.User;
import com.github.b3kt.infrastructure.persistence.entity.UserEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbKaryawanEntity;
import com.github.b3kt.infrastructure.persistence.repository.UserEntityRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbKaryawanRepository;
import com.github.b3kt.infrastructure.repository.UserRepository;
import com.github.b3kt.infrastructure.security.JwtTokenService;
import com.github.b3kt.infrastructure.security.PasswordEncoder;
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
    private JsonWebToken jwt;

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
            when(jwtTokenService.generateRefreshToken(any(User.class))).thenReturn("refresh123");
            when(jwtTokenService.getTokenExpirationSeconds()).thenReturn(86400L);

            LoginResponse result = authService.login("admin", "password");

            assertNotNull(result);
            assertEquals("token123", result.getToken());
            assertEquals("refresh123", result.getRefreshToken());
            assertEquals("admin", result.getUsername());
            assertEquals(86400L, result.getExpiresIn());
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
        @DisplayName("Should throw when user inactive")
        void testLogin_userInactive() {
            testUser.setActive(false);
            when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));

            AuthenticationException ex = assertThrows(AuthenticationException.class,
                    () -> authService.login("admin", "password"));

            assertEquals("User account is not active", ex.getMessage());
        }

        @Test
        @DisplayName("Should throw when user has null username")
        void testLogin_nullUsername() {
            testUser.setUsername(null);
            when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));

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
            when(jwtTokenService.generateRefreshToken(any(User.class))).thenReturn("refresh123");
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
            when(jwtTokenService.generateRefreshToken(any(User.class))).thenReturn("refresh123");
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
            when(jwtTokenService.validateRefreshToken("refresh_token")).thenReturn("admin");
            when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));
            when(jwtTokenService.generateToken(any(User.class))).thenReturn("new_token");
            when(jwtTokenService.generateRefreshToken(any(User.class))).thenReturn("new_refresh");
            when(jwtTokenService.getTokenExpirationSeconds()).thenReturn(86400L);

            LoginResponse result = authService.refreshToken("refresh_token");

            assertNotNull(result);
            assertEquals("new_token", result.getToken());
            assertEquals("new_refresh", result.getRefreshToken());
        }

        @Test
        @DisplayName("Should throw when refresh token invalid")
        void testRefreshToken_invalid() {
            when(jwtTokenService.validateRefreshToken("invalid")).thenReturn(null);

            AuthenticationException ex = assertThrows(AuthenticationException.class,
                    () -> authService.refreshToken("invalid"));

            assertEquals("Invalid or expired refresh token", ex.getMessage());
        }

        @Test
        @DisplayName("Should throw when user not found after refresh")
        void testRefreshToken_userNotFound() {
            when(jwtTokenService.validateRefreshToken("refresh_token")).thenReturn("nonexistent");
            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            AuthenticationException ex = assertThrows(AuthenticationException.class,
                    () -> authService.refreshToken("refresh_token"));

            assertEquals("User not found", ex.getMessage());
        }

        @Test
        @DisplayName("Should throw when user inactive during refresh")
        void testRefreshToken_userInactive() {
            testUser.setActive(false);
            when(jwtTokenService.validateRefreshToken("refresh_token")).thenReturn("admin");
            when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));

            AuthenticationException ex = assertThrows(AuthenticationException.class,
                    () -> authService.refreshToken("refresh_token"));

            assertEquals("User account is not active", ex.getMessage());
        }

        @Test
        @DisplayName("Should enrich with karyawan info during refresh")
        void testRefreshToken_withKaryawan() {
            when(jwtTokenService.validateRefreshToken("refresh_token")).thenReturn("admin");
            when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));

            TbKaryawanEntity karyawan = new TbKaryawanEntity();
            karyawan.setId(42L);
            karyawan.setNamaKaryawan("Budi");
            when(tbKaryawanRepository.findByUsername("admin")).thenReturn(Optional.of(karyawan));
            when(jwtTokenService.generateToken(any(User.class))).thenReturn("new_token");
            when(jwtTokenService.generateRefreshToken(any(User.class))).thenReturn("new_refresh");
            when(jwtTokenService.getTokenExpirationSeconds()).thenReturn(86400L);

            LoginResponse result = authService.refreshToken("refresh_token");

            assertNotNull(result);
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
