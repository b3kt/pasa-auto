package com.github.b3kt.infrastructure.persistence.repository.pazaauto;

import com.github.b3kt.application.helper.PhoneNormalizer;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPelangganEntity;
import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheResult;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class TbPelangganRepository implements PanacheRepositoryBase<TbPelangganEntity, Long> {

    @CacheResult(cacheName = "pelanggan-cache")
    public Optional<TbPelangganEntity> findByIdCached(Long id) {
        return find("id = ?1 and deletedAt is null", id).firstResultOptional();
    }

    @CacheInvalidate(cacheName = "pelanggan-cache")
    public void invalidatePelangganCache() {
        // Cache invalidation triggered on write operations
    }

    /**
     * Matches a customer's self-reported phone/email against the contact fields on record, for the
     * customer portal's "link my account" search. Phones are compared in canonical form
     * ({@link PhoneNormalizer}), so {@code +62 812-3456} matches a stored {@code 08123456}. Not cached:
     * a customer-supplied value used as a query parameter shouldn't populate the shared lookup cache.
     */
    public List<TbPelangganEntity> findByContact(String phone, String email) {
        String normalizedPhone = PhoneNormalizer.normalize(phone);
        String normalizedEmail = email == null || email.isBlank() ? null : email.trim();
        if (normalizedPhone == null && normalizedEmail == null) {
            return List.of();
        }
        if (normalizedPhone != null && normalizedEmail != null) {
            return find("deletedAt is null and ((noHp = ?1 or noTelepon = ?1) or lower(email) = ?2)",
                    normalizedPhone, normalizedEmail.toLowerCase()).list();
        }
        if (normalizedPhone != null) {
            return find("deletedAt is null and (noHp = ?1 or noTelepon = ?1)", normalizedPhone).list();
        }
        return find("deletedAt is null and lower(email) = ?1", normalizedEmail.toLowerCase()).list();
    }

    @CacheResult(cacheName = "pelanggan-cache")
    public List<TbPelangganEntity> findAllCached() {
        return find("deletedAt is null").list();
    }
}
