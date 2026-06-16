package com.github.b3kt.infrastructure.repository;

import com.github.b3kt.domain.model.User;
import com.github.b3kt.infrastructure.persistence.entity.UserEntity;
import com.github.b3kt.infrastructure.persistence.repository.UserEntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JpaUserRepositoryTest {

    @Mock
    UserEntityRepository userEntityRepository;

    @InjectMocks
    JpaUserRepository jpaUserRepository;

    private User testUser;
    private UserEntity testEntity;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("hash");
        testUser.setActive(true);
        testUser.setRoles(Set.of());

        testEntity = new UserEntity();
        testEntity.setUsername("testuser");
        testEntity.setEmail("test@example.com");
        testEntity.setPasswordHash("hash");
        testEntity.setActive(true);
    }

    @Test
    @DisplayName("findByUsername returns domain user when entity exists")
    void findByUsernameFound() {
        when(userEntityRepository.findByUsername("testuser")).thenReturn(Optional.of(testEntity));

        Optional<User> result = jpaUserRepository.findByUsername("testuser");

        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
    }

    @Test
    @DisplayName("findByUsername returns empty when entity not found")
    void findByUsernameNotFound() {
        when(userEntityRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        Optional<User> result = jpaUserRepository.findByUsername("nonexistent");

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("save creates new entity when user does not exist")
    void saveNewUser() {
        when(userEntityRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        User result = jpaUserRepository.save(testUser);

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        verify(userEntityRepository).persist(any(UserEntity.class));
    }

    @Test
    @DisplayName("save updates existing entity when user exists")
    void saveExistingUser() {
        when(userEntityRepository.findByUsername("testuser")).thenReturn(Optional.of(testEntity));
        doNothing().when(userEntityRepository).persist(any(UserEntity.class));

        User result = jpaUserRepository.save(testUser);

        assertNotNull(result);
        verify(userEntityRepository).persist(any(UserEntity.class));
    }

    @Test
    @DisplayName("existsByUsername delegates to repository")
    void existsByUsername() {
        when(userEntityRepository.existsByUsername("testuser")).thenReturn(true);

        assertTrue(jpaUserRepository.existsByUsername("testuser"));
        verify(userEntityRepository).existsByUsername("testuser");
    }
}
