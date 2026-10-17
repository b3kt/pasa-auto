package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.infrastructure.persistence.entity.customer.CustomerAccountEntity;
import com.github.b3kt.infrastructure.persistence.entity.customer.CustomerPelangganLinkRequestEntity;
import com.github.b3kt.infrastructure.persistence.entity.customer.CustomerVehicleClaimRequestEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPelangganEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPelangganKendaraanEntity;
import com.github.b3kt.infrastructure.persistence.repository.customer.CustomerAccountRepository;
import com.github.b3kt.infrastructure.persistence.repository.customer.CustomerPelangganLinkRequestRepository;
import com.github.b3kt.infrastructure.persistence.repository.customer.CustomerVehicleClaimRequestRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbPelangganKendaraanRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbPelangganRepository;
import com.github.b3kt.infrastructure.repository.JpaPelangganRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Merges a duplicate customer record into the one that survives (ADR-0011). Everything that describes
 * the same person moves to the survivor - ownership history, pending vehicle claims, pending link
 * requests and the linked portal account - and the duplicate is soft-deleted with
 * {@code merged_into_id} pointing at the survivor, so historical references that still name it
 * ({@code tb_spk}, {@code tb_penjualan}, {@code tb_customer_bookings}) can be resolved.
 */
@ApplicationScoped
public class PelangganMergeService {

    @Inject
    TbPelangganRepository pelangganRepository;

    @Inject
    TbPelangganKendaraanRepository ownershipRepository;

    @Inject
    CustomerAccountRepository accountRepository;

    @Inject
    CustomerVehicleClaimRequestRepository claimRequestRepository;

    @Inject
    CustomerPelangganLinkRequestRepository linkRequestRepository;

    @Inject
    JpaPelangganRepository pelangganNopolRepository;

    @Transactional
    public void merge(Long duplicateId, Long survivorId) {
        if (duplicateId == null || survivorId == null || Objects.equals(duplicateId, survivorId)) {
            throw new IllegalArgumentException("Merge needs two different customer records");
        }
        TbPelangganEntity duplicate = pelangganRepository.findByIdOptional(duplicateId)
                .filter(p -> p.getDeletedAt() == null)
                .orElseThrow(() -> new IllegalArgumentException("Pelanggan not found: " + duplicateId));
        TbPelangganEntity survivor = pelangganRepository.findByIdOptional(survivorId)
                .filter(p -> p.getDeletedAt() == null)
                .orElseThrow(() -> new IllegalArgumentException("Pelanggan not found: " + survivorId));

        moveAccount(duplicateId, survivorId);

        // The partial unique index allows one current owner per plate, so a current row of the
        // duplicate can never collide with a current row of the survivor: moving is always safe.
        List<TbPelangganKendaraanEntity> ownership = ownershipRepository.findByPelangganId(duplicateId);
        for (TbPelangganKendaraanEntity row : ownership) {
            row.setPelangganId(survivorId);
            ownershipRepository.getEntityManager().merge(row);
            pelangganNopolRepository.invalidateByNopol(row.getNopol());
        }

        claimRequestRepository.update("pelangganId = ?1 where pelangganId = ?2 and status = ?3",
                survivorId, duplicateId, CustomerVehicleClaimRequestEntity.STATUS_PENDING);
        linkRequestRepository.update("pelangganId = ?1 where pelangganId = ?2 and status = ?3",
                survivorId, duplicateId, CustomerPelangganLinkRequestEntity.STATUS_PENDING);

        duplicate.setMergedIntoId(survivor.getId());
        duplicate.setDeletedAt(LocalDateTime.now());
        pelangganRepository.getEntityManager().merge(duplicate);
        pelangganRepository.invalidatePelangganCache();
    }

    /** Keeps the strict 1:1 account link: the survivor may not already belong to another account. */
    private void moveAccount(Long duplicateId, Long survivorId) {
        Optional<CustomerAccountEntity> duplicateHolder = accountRepository.findByPelangganId(duplicateId);
        if (duplicateHolder.isEmpty()) {
            return;
        }
        Optional<CustomerAccountEntity> survivorHolder = accountRepository.findByPelangganId(survivorId);
        if (survivorHolder.isPresent()) {
            throw new IllegalArgumentException("Both records are linked to customer accounts; unlink one first");
        }
        CustomerAccountEntity account = duplicateHolder.get();
        account.setPelangganId(survivorId);
        account.setLinkedAt(LocalDateTime.now());
        accountRepository.getEntityManager().merge(account);
    }
}
