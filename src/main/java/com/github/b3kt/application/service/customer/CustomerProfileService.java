package com.github.b3kt.application.service.customer;

import com.github.b3kt.application.dto.customer.CustomerProfileDto;
import com.github.b3kt.application.dto.customer.UpdateProfileRequest;
import com.github.b3kt.infrastructure.persistence.entity.customer.CustomerAccountEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPelangganEntity;
import com.github.b3kt.infrastructure.persistence.repository.customer.CustomerAccountRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbPelangganRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;

@ApplicationScoped
public class CustomerProfileService {

    @Inject
    CustomerAccountRepository accountRepository;

    @Inject
    TbPelangganRepository pelangganRepository;

    public CustomerProfileDto getProfile(Long customerAccountId) {
        CustomerAccountEntity account = accountRepository.findByIdOptional(customerAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        CustomerProfileDto dto = new CustomerProfileDto();
        dto.setAccountId(account.getId());
        dto.setEmail(account.getEmail());
        dto.setPhone(account.getPhone());
        dto.setFullName(account.getFullName());
        dto.setLinked(account.isLinked());

        if (account.isLinked()) {
            pelangganRepository.findByIdCached(account.getPelangganId()).ifPresent(p -> {
                fillPelanggan(dto, p);
                // Once linked, the customer record is authoritative for the name (ADR-0011).
                if (p.getNamaPelanggan() != null && !p.getNamaPelanggan().isBlank()) {
                    dto.setFullName(p.getNamaPelanggan());
                }
            });
        }
        return dto;
    }

    /**
     * Updates the caller's own tb_pelanggan record, resolving it from the verified account id
     * ({@link com.github.b3kt.infrastructure.security.CustomerPrincipal}) rather than a
     * client-supplied value, so a customer can only ever edit their own record. When the account
     * has no linked record yet - or its record was removed - a new one is created and linked, so a
     * freshly registered customer can complete their profile without a separate linking step.
     */
    @Transactional
    public void updateProfile(Long customerAccountId, UpdateProfileRequest request) {
        CustomerAccountEntity account = accountRepository.findByIdOptional(customerAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        TbPelangganEntity p = resolveOrCreatePelanggan(account);

        p.setNamaPelanggan(request.getNamaPelanggan().trim());
        p.setAlamat(request.getAlamat());
        p.setTelepon(request.getTelepon());
        p.setNoHp(request.getNoHp());
        p.setNoTelepon(request.getNoTelepon());
        p.setEmail(request.getEmail());
        p.setKota(request.getKota());
        p.setKodePos(request.getKodePos());
        p.setContactPerson(request.getContactPerson());

        // findByIdCached is used throughout (vehicle lookups, link search, bookings) - without this,
        // those would keep serving the pre-update row for as long as the cache entry lives.
        pelangganRepository.invalidatePelangganCache();
    }

    private TbPelangganEntity resolveOrCreatePelanggan(CustomerAccountEntity account) {
        if (account.getPelangganId() != null) {
            var existing = pelangganRepository.findByIdOptional(account.getPelangganId());
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        TbPelangganEntity pelanggan = PortalPelanggan.create(account);
        pelangganRepository.persist(pelanggan);
        account.setPelangganId(pelanggan.getId());
        account.setLinkedAt(LocalDateTime.now());
        return pelanggan;
    }

    private void fillPelanggan(CustomerProfileDto dto, TbPelangganEntity p) {
        dto.setPelangganId(p.getId());
        dto.setNamaPelanggan(p.getNamaPelanggan());
        dto.setAlamat(p.getAlamat());
        dto.setTelepon(p.getNoTelepon());
        dto.setNoHp(p.getNoHp());
        dto.setNoTelepon(p.getNoTelepon());
        dto.setPelangganEmail(p.getEmail());
        dto.setKota(p.getKota());
        dto.setKodePos(p.getKodePos());
        dto.setContactPerson(p.getContactPerson());
    }
}
