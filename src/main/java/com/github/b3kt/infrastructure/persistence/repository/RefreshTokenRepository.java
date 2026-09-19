package com.github.b3kt.infrastructure.persistence.repository;

import com.github.b3kt.infrastructure.persistence.entity.RefreshTokenEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * JPA Repository for RefreshTokenEntity using Panache.
 */
@ApplicationScoped
public class RefreshTokenRepository implements PanacheRepositoryBase<RefreshTokenEntity, String> {

    /**
     * Find a token and lock its row, so concurrent refreshes with the same token are serialized.
     */
    public Optional<RefreshTokenEntity> findByIdForUpdate(String id) {
        return findByIdOptional(id, LockModeType.PESSIMISTIC_WRITE);
    }

    public long revokeFamily(String familyId, LocalDateTime now) {
        return update("revokedAt = ?1 where familyId = ?2 and revokedAt is null", now, familyId);
    }

    public long revokeAllForUser(String username, LocalDateTime now) {
        return update("revokedAt = ?1 where username = ?2 and revokedAt is null", now, username);
    }

    public long deleteExpiredForUser(String username, LocalDateTime now) {
        return delete("username = ?1 and expiresAt < ?2", username, now);
    }
}
