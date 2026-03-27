package com.github.b3kt.infrastructure.repository;

import com.github.b3kt.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserRepositoryTest {

    @Mock
    private JpaUserRepository jpaUserRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("password123");
        testUser.setActive(true);
    }

    @Test
    @DisplayName("Should find user by username successfully")
    void testFindByUsernameSuccess() {
        when(jpaUserRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        Optional<User> result = jpaUserRepository.findByUsername("testuser");

        assertTrue(result.isPresent());
        User foundUser = result.get();
        assertEquals(testUser.getUsername(), foundUser.getUsername());
        assertEquals(testUser.getEmail(), foundUser.getEmail());
        assertEquals(testUser.getPasswordHash(), foundUser.getPasswordHash());
        assertEquals(testUser.isActive(), foundUser.isActive());
        
        verify(jpaUserRepository).findByUsername("testuser");
    }

    @Test
    @DisplayName("Should return empty when user not found by username")
    void testFindByUsernameNotFound() {
        when(jpaUserRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        Optional<User> result = jpaUserRepository.findByUsername("nonexistent");

        assertFalse(result.isPresent());
        verify(jpaUserRepository).findByUsername("nonexistent");
    }

    @Test
    @DisplayName("Should save user successfully")
    void testSave() {
        when(jpaUserRepository.save(any(User.class))).thenReturn(testUser);

        User result = jpaUserRepository.save(testUser);

        assertNotNull(result);
        assertEquals(testUser.getUsername(), result.getUsername());
        assertEquals(testUser.getEmail(), result.getEmail());
        assertEquals(testUser.getPasswordHash(), result.getPasswordHash());
        assertEquals(testUser.isActive(), result.isActive());
    }

    @Test
    @DisplayName("Should check if user exists by username - exists")
    void testExistsByUsernameTrue() {
        when(jpaUserRepository.existsByUsername("testuser")).thenReturn(true);

        boolean result = jpaUserRepository.existsByUsername("testuser");

        assertTrue(result);
        verify(jpaUserRepository).existsByUsername("testuser");
    }

    @Test
    @DisplayName("Should check if user exists by username - not exists")
    void testExistsByUsernameFalse() {
        when(jpaUserRepository.existsByUsername("nonexistent")).thenReturn(false);

        boolean result = jpaUserRepository.existsByUsername("nonexistent");

        assertFalse(result);
        verify(jpaUserRepository).existsByUsername("nonexistent");
    }

    @Test
    @DisplayName("Should handle null username gracefully")
    void testNullUsername() {
        assertDoesNotThrow(() -> jpaUserRepository.findByUsername(null));
        assertDoesNotThrow(() -> jpaUserRepository.existsByUsername(null));
    }

    @Test
    @DisplayName("Should handle empty username gracefully")
    void testEmptyUsername() {
        assertDoesNotThrow(() -> jpaUserRepository.findByUsername(""));
        assertDoesNotThrow(() -> jpaUserRepository.existsByUsername(""));
    }

    @Test
    @DisplayName("Should handle null user save gracefully")
    void testNullUserSave() {
        assertDoesNotThrow(() -> jpaUserRepository.save(null));
    }
}
