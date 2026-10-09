package com.github.b3kt.infrastructure.persistence.repository.customer;

import com.github.b3kt.infrastructure.persistence.entity.customer.CustomerPelangganLinkRequestEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class CustomerPelangganLinkRequestRepository implements PanacheRepositoryBase<CustomerPelangganLinkRequestEntity, Long> {

    public Optional<CustomerPelangganLinkRequestEntity> findPendingByCustomerAccount(Long customerAccountId) {
        return find("customerAccountId = ?1 and status = ?2",
                customerAccountId, CustomerPelangganLinkRequestEntity.STATUS_PENDING).firstResultOptional();
    }

    public List<CustomerPelangganLinkRequestEntity> findByStatus(String status) {
        if (status == null || status.isBlank()) {
            return listAll();
        }
        return find("status", status).list();
    }
}
