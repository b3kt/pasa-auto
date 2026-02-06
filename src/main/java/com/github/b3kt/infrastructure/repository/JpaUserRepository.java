package com.github.b3kt.infrastructure.repository;

import com.github.b3kt.domain.model.User;
import com.github.b3kt.infrastructure.persistence.entity.UserEntity;
import com.github.b3kt.infrastructure.persistence.repository.UserEntityRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.Optional;

/**
 * JPA implementation of UserRepository.
 * This is the default repository implementation using database persistence.
 * To use in-memory instead, set: app.repository.type=memory
 */
@Default
@ApplicationScoped
public class JpaUserRepository implements UserRepository {

    @Inject
    UserEntityRepository userEntityRepository;

    @Override
    @Transactional
    public Optional<User> findByUsername(String username) {
        return userEntityRepository.findByUsername(username)
                .map(UserEntity::toDomain);
    }

    @Override
    @Transactional
    public User save(User user) {
        UserEntity entity = userEntityRepository.findByUsername(user.getUsername())
                .orElse(UserEntity.fromDomain(user));
        
        // Update entity with domain values
        entity.setEmail(user.getEmail());
        entity.setPasswordHash(user.getPasswordHash());
        entity.setRoles(user.getRoles());
        entity.setActive(user.isActive());
        
        userEntityRepository.persist(entity);
        return entity.toDomain();
    }

    @Override
    public boolean existsByUsername(String username) {
        return userEntityRepository.existsByUsername(username);
    }
}

