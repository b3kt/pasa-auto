package com.github.b3kt.application.service.customer;

import com.github.b3kt.application.dto.customer.CustomerProfileDto;
import com.github.b3kt.application.dto.customer.UpdateProfileRequest;
import com.github.b3kt.application.helper.PhoneNormalizer;
import com.github.b3kt.infrastructure.persistence.entity.customer.CustomerAccountEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPelangganEntity;
import com.github.b3kt.infrastructure.persistence.repository.customer.CustomerAccountRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbPelangganRepository;
import com.github.b3kt.infrastructure.repository.JpaPelangganRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@ApplicationScoped
public class CustomerProfileService {

    @Inject
    CustomerAccountRepository accountRepository;

    @Inject
    TbPelangganRepository pelangganRepository;

    @Inject
    JpaPelangganRepository pelangganCache;

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
                fillPelanggan(dto, p, account);
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
     *
     * <p>Contact details live on the account (ADR-0012): the mobile number is written to
     * {@code tb_customer_accounts.phone}. The email is the login email and is not changed here; the
     * deprecated landline fields of the request are ignored.
     */
    @Transactional
    public void updateProfile(Long customerAccountId, UpdateProfileRequest request) {
        CustomerAccountEntity account = accountRepository.findByIdOptional(customerAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        TbPelangganEntity p = resolveOrCreatePelanggan(account);

        updateAccountPhone(account, request.getNoHp());

        p.setNamaPelanggan(request.getNamaPelanggan().trim());
        p.setAlamat(request.getAlamat());
        p.setKota(request.getKota());
        p.setKodePos(request.getKodePos());
        p.setContactPerson(request.getContactPerson());

        // findByIdCached is used throughout (this endpoint's own response, the refreshed display
        // name, bookings) and the staff by-nopol lookups hold a copy per plate - without this, all of
        // those keep serving the pre-update record.
        pelangganCache.invalidatePelanggan(p.getId());
    }

    /** The phone is also a login identifier, so it must stay unique across accounts. */
    private void updateAccountPhone(CustomerAccountEntity account, String requestedPhone) {
        String phone = PhoneNormalizer.normalize(requestedPhone);
        if (Objects.equals(phone, account.getPhone())) {
            return;
        }
        if (requestedPhone != null && !requestedPhone.isBlank() && !PhoneNormalizer.isValid(phone)) {
            throw new IllegalArgumentException("Phone number is not valid");
        }
        if (phone == null && account.getEmail() == null) {
            throw new IllegalArgumentException("A phone number is required: it is how you sign in");
        }
        if (phone != null && accountRepository.findByPhone(phone)
                .filter(other -> !Objects.equals(other.getId(), account.getId()))
                .isPresent()) {
            throw new IllegalArgumentException("Phone number is already registered");
        }
        account.setPhone(phone);
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

    private void fillPelanggan(CustomerProfileDto dto, TbPelangganEntity p, CustomerAccountEntity account) {
        dto.setPelangganId(p.getId());
        dto.setNamaPelanggan(p.getNamaPelanggan());
        dto.setAlamat(p.getAlamat());
        // Contact details come from the account (ADR-0012); the landline is deprecated and left null.
        dto.setNoHp(account.getPhone());
        dto.setPelangganEmail(account.getEmail());
        dto.setKota(p.getKota());
        dto.setKodePos(p.getKodePos());
        dto.setContactPerson(p.getContactPerson());
    }
}
