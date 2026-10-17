package com.github.b3kt.infrastructure.persistence.repository.customer;

import com.github.b3kt.application.helper.PhoneNormalizer;
import com.github.b3kt.infrastructure.persistence.entity.customer.CustomerAccountEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class CustomerAccountRepository implements PanacheRepositoryBase<CustomerAccountEntity, Long> {

    public Optional<CustomerAccountEntity> findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return find("lower(email) = ?1", email.trim().toLowerCase()).firstResultOptional();
    }

    public Optional<CustomerAccountEntity> findByPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return Optional.empty();
        }
        // Canonical form first (ADR-0011); the raw value still matches a legacy row V35 could not
        // canonicalize because it collided with another account.
        String canonical = PhoneNormalizer.normalize(phone);
        return find("phone = ?1 or phone = ?2", canonical == null ? phone.trim() : canonical, phone.trim())
                .firstResultOptional();
    }

    /** Live accounts linked to any of {@code pelangganIds}, for batch reads (one query per page). */
    public List<CustomerAccountEntity> findByPelangganIds(Collection<Long> pelangganIds) {
        if (pelangganIds == null || pelangganIds.isEmpty()) {
            return List.of();
        }
        return find("pelangganId in ?1 and deletedAt is null", pelangganIds).list();
    }

    public Optional<CustomerAccountEntity> findByPelangganId(Long pelangganId) {
        if (pelangganId == null) {
            return Optional.empty();
        }
        return find("pelangganId", pelangganId).firstResultOptional();
    }
}
