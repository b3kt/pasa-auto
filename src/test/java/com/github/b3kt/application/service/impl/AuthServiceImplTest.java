package com.github.b3kt.application.service.impl;

import com.github.b3kt.application.dto.LoginResponse;
import com.github.b3kt.application.dto.UserInfo;
import com.github.b3kt.domain.exception.AuthenticationException;
import com.github.b3kt.domain.model.User;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbKaryawanEntity;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbKaryawanRepository;
import com.github.b3kt.infrastructure.repository.UserRepository;
import com.github.b3kt.infrastructure.security.JwtTokenService;
import com.github.b3kt.infrastructure.security.PasswordEncoder;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AuthServiceImplTest {

    private UserRepository userRepository;
    private TbKaryawanRepository tbKaryawanRepository;
    private JwtTokenService jwtTokenService;
    private PasswordEncoder passwordEncoder;
    private AuthServiceImpl authService;
    private User testUser;
    private TbKaryawanEntity testKaryawan;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        tbKaryawanRepository = mock(TbKaryawanRepository.class);
        jwtTokenService = mock(JwtTokenService.class);
        passwordEncoder = mock(PasswordEncoder.class);

        authService = new AuthServiceImpl(
            userRepository,
            tbKaryawanRepository,
            jwtTokenService,
            passwordEncoder
        );

        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("password123");
        testUser.setActive(true);

        testKaryawan = new TbKaryawanEntity();
        testKaryawan.setId(100L);
        testKaryawan.setNamaKaryawan("Test Employee");
    }

    @Test
    @DisplayName("Should successfully login with valid credentials")
    void testLoginSuccess() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(tbKaryawanRepository.findByUsername("testuser")).thenReturn(Optional.of(testKaryawan));
        when(passwordEncoder.matches("password123", "password123")).thenReturn(true);
        when(jwtTokenService.generateToken(any(User.class))).thenReturn("jwt-token");
        when(jwtTokenService.generateRefreshToken(any(User.class))).thenReturn("refresh-token");
        when(jwtTokenService.getTokenExpirationSeconds()).thenReturn(3600L);

        LoginResponse response = authService.login("testuser", "password123");

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals("testuser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());
        assertEquals(3600L, response.getExpiresIn());
        
        verify(userRepository).findByUsername("testuser");
        verify(tbKaryawanRepository).findByUsername("testuser");
        verify(jwtTokenService).generateToken(testUser);
        verify(jwtTokenService).generateRefreshToken(testUser);
    }

    @Test
    @DisplayName("Should throw AuthenticationException for invalid username")
    void testLoginInvalidUsername() {
        when(userRepository.findByUsername("invalid")).thenReturn(Optional.empty());

        AuthenticationException exception = assertThrows(
            AuthenticationException.class,
            () -> authService.login("invalid", "password")
        );
        
        assertEquals("Invalid username or password", exception.getMessage());
        verify(userRepository).findByUsername("invalid");
        verifyNoInteractions(jwtTokenService);
    }

    @Test
    @DisplayName("Should throw AuthenticationException for inactive user")
    void testLoginInactiveUser() {
        testUser.setActive(false);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        AuthenticationException exception = assertThrows(
            AuthenticationException.class,
            () -> authService.login("testuser", "password123")
        );
        
        assertEquals("User account is not active", exception.getMessage());
        verify(userRepository).findByUsername("testuser");
        verifyNoInteractions(jwtTokenService);
    }

    @Test
    @DisplayName("Should throw AuthenticationException for wrong password")
    void testLoginWrongPassword() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpassword", "password123")).thenReturn(false);

        AuthenticationException exception = assertThrows(
            AuthenticationException.class,
            () -> authService.login("testuser", "wrongpassword")
        );
        
        assertEquals("Invalid username or password", exception.getMessage());
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    @DisplayName("Should login successfully without karyawan info")
    void testLoginWithoutKaryawan() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(tbKaryawanRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        when(passwordEncoder.matches("password123", "password123")).thenReturn(true);
        when(jwtTokenService.generateToken(any(User.class))).thenReturn("jwt-token");
        when(jwtTokenService.generateRefreshToken(any(User.class))).thenReturn("refresh-token");
        when(jwtTokenService.getTokenExpirationSeconds()).thenReturn(3600L);

        LoginResponse response = authService.login("testuser", "password123");

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals("refresh-token", response.getRefreshToken());
        
        verify(userRepository).findByUsername("testuser");
        verify(tbKaryawanRepository).findByUsername("testuser");
        verify(jwtTokenService).generateToken(testUser);
        verify(jwtTokenService).generateRefreshToken(testUser);
    }

    @Test
    @DisplayName("Should successfully refresh token")
    void testRefreshTokenSuccess() {
        when(jwtTokenService.validateRefreshToken("valid-refresh-token")).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(tbKaryawanRepository.findByUsername("testuser")).thenReturn(Optional.of(testKaryawan));
        when(jwtTokenService.generateToken(any(User.class))).thenReturn("new-jwt-token");
        when(jwtTokenService.generateRefreshToken(any(User.class))).thenReturn("new-refresh-token");
        when(jwtTokenService.getTokenExpirationSeconds()).thenReturn(3600L);

        LoginResponse response = authService.refreshToken("valid-refresh-token");

        assertNotNull(response);
        assertEquals("new-jwt-token", response.getToken());
        assertEquals("new-refresh-token", response.getRefreshToken());
        assertEquals("testuser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());
        assertEquals(3600L, response.getExpiresIn());
        
        verify(jwtTokenService).validateRefreshToken("valid-refresh-token");
        verify(userRepository).findByUsername("testuser");
        verify(tbKaryawanRepository).findByUsername("testuser");
        verify(jwtTokenService).generateToken(testUser);
        verify(jwtTokenService).generateRefreshToken(testUser);
    }

    @Test
    @DisplayName("Should throw AuthenticationException for invalid refresh token")
    void testRefreshTokenInvalid() {
        when(jwtTokenService.validateRefreshToken("invalid-token")).thenReturn(null);

        AuthenticationException exception = assertThrows(
            AuthenticationException.class,
            () -> authService.refreshToken("invalid-token")
        );
        
        assertEquals("Invalid or expired refresh token", exception.getMessage());
        verify(jwtTokenService).validateRefreshToken("invalid-token");
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("Should throw AuthenticationException when user not found during refresh")
    void testRefreshTokenUserNotFound() {
        when(jwtTokenService.validateRefreshToken("valid-token")).thenReturn("nonexistent");
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        AuthenticationException exception = assertThrows(
            AuthenticationException.class,
            () -> authService.refreshToken("valid-token")
        );
        
        assertEquals("User not found", exception.getMessage());
        verify(jwtTokenService).validateRefreshToken("valid-token");
        verify(userRepository).findByUsername("nonexistent");
    }

    @Test
    @DisplayName("Should throw AuthenticationException for inactive user during refresh")
    void testRefreshTokenInactiveUser() {
        testUser.setActive(false);
        when(jwtTokenService.validateRefreshToken("valid-token")).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        AuthenticationException exception = assertThrows(
            AuthenticationException.class,
            () -> authService.refreshToken("valid-token")
        );
        
        assertEquals("User account is not active", exception.getMessage());
        verify(jwtTokenService).validateRefreshToken("valid-token");
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    @DisplayName("Should get user info from JWT token")
    void testGetUserInfo() {
        JsonWebToken jwt = mock(JsonWebToken.class);
        UserInfo expectedUserInfo = new UserInfo();
        expectedUserInfo.setUsername("testuser");
        expectedUserInfo.setEmail("test@example.com");
        
        when(jwtTokenService.extractUserInfo(jwt)).thenReturn(expectedUserInfo);

        UserInfo result = authService.getUserInfo(jwt);

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
        verify(jwtTokenService).extractUserInfo(jwt);
    }

    @Test
    @DisplayName("Should throw exception for null username")
    void testNullUsername() {
        assertThrows(AuthenticationException.class,
            () -> authService.login(null, "password"));
    }
}
