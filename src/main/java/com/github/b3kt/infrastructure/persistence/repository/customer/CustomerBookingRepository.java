package com.github.b3kt.infrastructure.persistence.repository.customer;

import com.github.b3kt.infrastructure.persistence.entity.customer.CustomerBookingEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class CustomerBookingRepository implements PanacheRepositoryBase<CustomerBookingEntity, Long> {

    public List<CustomerBookingEntity> findByPelangganId(Long pelangganId) {
        return find("pelangganId = ?1 order by requestedDateTime desc", pelangganId).list();
    }

    public List<CustomerBookingEntity> findByStatus(String status) {
        if (status == null || status.isBlank()) {
            return find("order by requestedDateTime desc").list();
        }
        return find("status = ?1 order by requestedDateTime desc", status).list();
    }
}
