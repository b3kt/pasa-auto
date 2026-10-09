package com.github.b3kt.infrastructure.persistence.repository.customer;

import com.github.b3kt.infrastructure.persistence.entity.customer.CustomerRefreshTokenEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.Optional;

/** Mirrors {@code RefreshTokenRepository}, keyed by customer account id instead of username. */
@ApplicationScoped
public class CustomerRefreshTokenRepository implements PanacheRepositoryBase<CustomerRefreshTokenEntity, String> {

    public Optional<CustomerRefreshTokenEntity> findByIdForUpdate(String id) {
        return findByIdOptional(id, LockModeType.PESSIMISTIC_WRITE);
    }

    public long revokeFamily(String familyId, LocalDateTime now) {
        return update("revokedAt = ?1 where familyId = ?2 and revokedAt is null", now, familyId);
    }

    public long revokeAllForAccount(Long customerAccountId, LocalDateTime now) {
        return update("revokedAt = ?1 where customerAccountId = ?2 and revokedAt is null", now, customerAccountId);
    }

    public long deleteExpiredForAccount(Long customerAccountId, LocalDateTime now) {
        return delete("customerAccountId = ?1 and expiresAt < ?2", customerAccountId, now);
    }
}
