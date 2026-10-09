package com.github.b3kt.infrastructure.persistence.repository.customer;

import com.github.b3kt.infrastructure.persistence.entity.customer.CustomerAccountEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

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
        return find("phone", phone.trim()).firstResultOptional();
    }

    public Optional<CustomerAccountEntity> findByPelangganId(Long pelangganId) {
        if (pelangganId == null) {
            return Optional.empty();
        }
        return find("pelangganId", pelangganId).firstResultOptional();
    }
}
