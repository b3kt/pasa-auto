package com.github.b3kt.infrastructure.repository;

import com.github.b3kt.domain.model.User;
import java.util.Optional;

/**
 * Repository interface for user persistence operations.
 * This follows the Repository pattern for data access.
 */
public interface UserRepository {
    
    /**
     * Find user by username.
     * 
     * @param username the username
     * @return Optional containing User if found
     */
    Optional<User> findByUsername(String username);
    
    /**
     * Save or update a user.
     * 
     * @param user the user to save
     * @return the saved user
     */
    User save(User user);
    
    /**
     * Check if user exists by username.
     * 
     * @param username the username
     * @return true if user exists
     */
    boolean existsByUsername(String username);
}

