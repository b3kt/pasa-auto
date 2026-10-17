package com.github.b3kt.infrastructure.persistence.repository.customer;

import com.github.b3kt.infrastructure.persistence.entity.customer.CustomerVehicleClaimRequestEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class CustomerVehicleClaimRequestRepository implements PanacheRepositoryBase<CustomerVehicleClaimRequestEntity, Long> {

    public Optional<CustomerVehicleClaimRequestEntity> findPendingByCustomerAccount(Long customerAccountId) {
        return find("customerAccountId = ?1 and status = ?2",
                customerAccountId, CustomerVehicleClaimRequestEntity.STATUS_PENDING).firstResultOptional();
    }

    public Optional<CustomerVehicleClaimRequestEntity> findPendingByNopol(String nopol) {
        return find("nopol = ?1 and status = ?2",
                nopol, CustomerVehicleClaimRequestEntity.STATUS_PENDING).firstResultOptional();
    }

    public List<CustomerVehicleClaimRequestEntity> findByStatus(String status) {
        if (status == null || status.isBlank()) {
            return listAll();
        }
        return find("status", status).list();
    }
}
