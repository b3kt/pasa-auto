package com.github.b3kt.infrastructure.repository;

import com.github.b3kt.domain.model.User;
import com.github.b3kt.infrastructure.persistence.entity.RoleEntity;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of UserRepository.
 * This is a demo implementation. Use JpaUserRepository for database
 * persistence.
 * To use this, set: app.repository.type=memory
 */
@Alternative
@ApplicationScoped
public class InMemoryUserRepository implements UserRepository {

    @ConfigProperty(name = "app.repository.type", defaultValue = "jpa")
    String repositoryType;

    private final Map<String, User> users = new ConcurrentHashMap<>();

    public InMemoryUserRepository() {
        // Only initialize if this repository is active
        if ("memory".equalsIgnoreCase(repositoryType)) {
            // Initialize with demo user
            Set<String> roles = new HashSet<>();
            roles.add("user");
            roles.add("admin");

            User demoUser = new User(
                    "admin",
                    "admin@example.com",
                    "admin123", // In production, this should be hashed
                    roles.stream().map(this::toRoleEntity).collect(Collectors.toSet()));
            users.put("admin", demoUser);
        }
    }

    private RoleEntity toRoleEntity(String role) {
        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setName(role);
        return roleEntity;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(users.get(username));
    }

    @Override
    public User save(User user) {
        users.put(user.getUsername(), user);
        return user;
    }

    @Override
    public boolean existsByUsername(String username) {
        return users.containsKey(username);
    }
}
