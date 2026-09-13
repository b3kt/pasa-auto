package com.github.b3kt.infrastructure.repository;

import com.github.b3kt.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryUserRepositoryTest {

    private InMemoryUserRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryUserRepository();
        repository.repositoryType = "memory";
        // Re-trigger constructor logic - need to call init manually since CDI isn't active
        // The constructor checks config property; we set field directly
        repository = new InMemoryUserRepository();
        repository.repositoryType = "memory";
        // Manually add test data since constructor won't fire without CDI
        User user = new User("testuser", "test@example.com", "hash", Set.of());
        repository.save(user);
    }

    @Test
    @DisplayName("findByUsername returns user when exists")
    void findByUsernameFound() {
        Optional<User> result = repository.findByUsername("testuser");
        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
    }

    @Test
    @DisplayName("findByUsername returns empty when not found")
    void findByUsernameNotFound() {
        Optional<User> result = repository.findByUsername("nonexistent");
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("save stores and returns user")
    void save() {
        User user = new User("newuser", "new@x.com", "hash2", Set.of());
        User result = repository.save(user);
        assertEquals("newuser", result.getUsername());
        assertTrue(repository.existsByUsername("newuser"));
    }

    @Test
    @DisplayName("save overwrites existing user")
    void saveOverwrite() {
        User updated = new User("testuser", "updated@x.com", "newhash", Set.of());
        repository.save(updated);
        Optional<User> result = repository.findByUsername("testuser");
        assertTrue(result.isPresent());
        assertEquals("updated@x.com", result.get().getEmail());
    }

    @Test
    @DisplayName("existsByUsername returns true for existing user")
    void existsByUsernameFound() {
        assertTrue(repository.existsByUsername("testuser"));
    }

    @Test
    @DisplayName("existsByUsername returns false for non-existing user")
    void existsByUsernameNotFound() {
        assertFalse(repository.existsByUsername("nonexistent"));
    }
}
