package com.github.b3kt.infrastructure.repository;

import com.github.b3kt.domain.model.User;
import com.github.b3kt.infrastructure.persistence.entity.UserEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbKaryawanEntity;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@QuarkusTest
class UserRepositoryTest {

    @InjectMock
    JpaUserRepository jpaUserRepository;

    @Inject
    UserRepository userRepository;

    private User testUser;
    private UserEntity testUserEntity;

    @BeforeEach
    void setUp() {
        testUser = new User();
//        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("password123");
        testUser.setActive(true);

        testUserEntity = new UserEntity();
        testUserEntity.setId(1L);
        testUserEntity.setUsername("testuser");
        testUserEntity.setEmail("test@example.com");
        testUserEntity.setPasswordHash("password123");
        testUserEntity.setActive(true);
    }

    @Test
    @DisplayName("Should find user by username successfully")
    void testFindByUsernameSuccess() {
        // Given
        when(jpaUserRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When
        Optional<User> result = userRepository.findByUsername("testuser");

        // Then
        assertTrue(result.isPresent());
        User foundUser = result.get();
//        assertEquals(testUser.getId(), foundUser.getId());
        assertEquals(testUser.getUsername(), foundUser.getUsername());
        assertEquals(testUser.getEmail(), foundUser.getEmail());
        assertEquals(testUser.getPasswordHash(), foundUser.getPasswordHash());
        assertEquals(testUser.isActive(), foundUser.isActive());
        
        verify(jpaUserRepository).findByUsername("testuser");
    }

    @Test
    @DisplayName("Should return empty when user not found by username")
    void testFindByUsernameNotFound() {
        // Given
        when(jpaUserRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When
        Optional<User> result = userRepository.findByUsername("nonexistent");

        // Then
        assertFalse(result.isPresent());
        verify(jpaUserRepository).findByUsername("nonexistent");
    }

    @Test
    @DisplayName("Should save user successfully")
    void testSave() {
        // Given
        when(jpaUserRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userRepository.save(testUser);

        // Then
        assertNotNull(result);
//        assertEquals(testUserEntity.getId(), result.getId());
        assertEquals(testUserEntity.getUsername(), result.getUsername());
        assertEquals(testUserEntity.getEmail(), result.getEmail());
        assertEquals(testUserEntity.getPasswordHash(), result.getPasswordHash());
        assertEquals(testUserEntity.isActive(), result.isActive());
        
//        verify(jpaUserRepository).persist(any(UserEntity.class));
    }

    @Test
    @DisplayName("Should check if user exists by username - exists")
    void testExistsByUsernameTrue() {
        // Given
        when(jpaUserRepository.existsByUsername("testuser")).thenReturn(true);

        // When
        boolean result = userRepository.existsByUsername("testuser");

        // Then
        assertTrue(result);
        verify(jpaUserRepository).existsByUsername("testuser");
    }

    @Test
    @DisplayName("Should check if user exists by username - not exists")
    void testExistsByUsernameFalse() {
        // Given
        when(jpaUserRepository.existsByUsername("nonexistent")).thenReturn(false);

        // When
        boolean result = userRepository.existsByUsername("nonexistent");

        // Then
        assertFalse(result);
        verify(jpaUserRepository).existsByUsername("nonexistent");
    }

    @Test
    @DisplayName("Should handle null username gracefully")
    void testNullUsername() {
        // When & Then
        assertDoesNotThrow(() -> userRepository.findByUsername(null));
        
        assertDoesNotThrow(() -> userRepository.existsByUsername(null));
    }

    @Test
    @DisplayName("Should handle empty username gracefully")
    void testEmptyUsername() {
        // When & Then
        assertDoesNotThrow(() -> userRepository.findByUsername(""));
        assertDoesNotThrow(() -> userRepository.existsByUsername(""));
    }

    @Test
    @DisplayName("Should handle null user save gracefully")
    void testNullUserSave() {
        // When & Then
        assertDoesNotThrow(
                () -> userRepository.save(null));
    }
}
