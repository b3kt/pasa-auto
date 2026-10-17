package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.infrastructure.persistence.entity.customer.CustomerAccountEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPelangganEntity;
import com.github.b3kt.infrastructure.persistence.repository.customer.CustomerAccountRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Resolves a customer's contact details (ADR-0012). A customer linked to a portal account is reached
 * through that account: {@code tb_customer_accounts.phone}/{@code email} are authoritative and the
 * {@code tb_pelanggan} copies are ignored. A walk-in customer without an account keeps
 * {@code tb_pelanggan.no_hp}/{@code email}. The landline ({@code no_telepon}, {@code tlpn}) is
 * deprecated and never returned.
 */
@ApplicationScoped
public class PelangganContactResolver {

    /** Effective contact of one customer; {@code fromAccount} tells who owns the values. */
    public record Contact(String noHp, String email, boolean fromAccount) {

        static Contact of(TbPelangganEntity pelanggan, CustomerAccountEntity account) {
            if (account != null) {
                return new Contact(account.getPhone(), account.getEmail(), true);
            }
            return new Contact(pelanggan.getNoHp(), pelanggan.getEmail(), false);
        }
    }

    @Inject
    CustomerAccountRepository accountRepository;

    public Contact resolve(TbPelangganEntity pelanggan) {
        if (pelanggan == null) {
            return new Contact(null, null, false);
        }
        CustomerAccountEntity account = pelanggan.getId() == null ? null
                : accountRepository.findByPelangganId(pelanggan.getId())
                        .filter(a -> a.getDeletedAt() == null)
                        .orElse(null);
        return Contact.of(pelanggan, account);
    }

    /** @return true when the customer's contact details are managed through a portal account. */
    public boolean isManagedByAccount(Long pelangganId) {
        return pelangganId != null && accountRepository.findByPelangganId(pelangganId)
                .filter(a -> a.getDeletedAt() == null)
                .isPresent();
    }

    /**
     * Overwrites the contact fields of detached read copies with the effective values, looking up
     * the accounts of the whole batch in one query. Never pass managed or cached instances: the
     * values would be flushed into, or served from, {@code tb_pelanggan}.
     */
    public void applyTo(Collection<TbPelangganEntity> detachedCopies) {
        if (detachedCopies == null || detachedCopies.isEmpty()) {
            return;
        }
        List<Long> ids = detachedCopies.stream().map(TbPelangganEntity::getId).filter(Objects::nonNull).toList();
        Map<Long, CustomerAccountEntity> accounts = accountRepository.findByPelangganIds(ids).stream()
                .collect(Collectors.toMap(CustomerAccountEntity::getPelangganId, Function.identity(), (a, b) -> a));
        for (TbPelangganEntity p : detachedCopies) {
            Contact contact = Contact.of(p, accounts.get(p.getId()));
            p.setNoHp(contact.noHp());
            p.setEmail(contact.email());
            p.setNoTelepon(null);
            p.setTelepon(null);
            p.setContactFromAccount(contact.fromAccount());
        }
    }
}
