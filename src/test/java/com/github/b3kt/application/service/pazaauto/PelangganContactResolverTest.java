package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.infrastructure.persistence.entity.customer.CustomerAccountEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPelangganEntity;
import com.github.b3kt.infrastructure.persistence.repository.customer.CustomerAccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PelangganContactResolverTest {

    @Mock
    CustomerAccountRepository accountRepository;

    @InjectMocks
    PelangganContactResolver resolver;

    private TbPelangganEntity pelanggan(Long id, String noHp, String email) {
        TbPelangganEntity p = new TbPelangganEntity();
        p.setId(id);
        p.setNoHp(noHp);
        p.setEmail(email);
        p.setNoTelepon("0215551234");
        p.setTelepon("0215551234");
        return p;
    }

    private CustomerAccountEntity account(Long pelangganId, String phone, String email) {
        CustomerAccountEntity a = new CustomerAccountEntity();
        a.setPelangganId(pelangganId);
        a.setPhone(phone);
        a.setEmail(email);
        return a;
    }

    @Test
    @DisplayName("a linked customer is reached through the account, even when the account has no phone")
    void linkedUsesAccount() {
        when(accountRepository.findByPelangganId(7L)).thenReturn(Optional.of(account(7L, null, "acct@x")));

        PelangganContactResolver.Contact contact = resolver.resolve(pelanggan(7L, "0811", "old@x"));

        assertNull(contact.noHp());
        assertEquals("acct@x", contact.email());
        assertTrue(contact.fromAccount());
    }

    @Test
    @DisplayName("a walk-in customer keeps the record's own contact")
    void walkInUsesRecord() {
        when(accountRepository.findByPelangganId(8L)).thenReturn(Optional.empty());

        PelangganContactResolver.Contact contact = resolver.resolve(pelanggan(8L, "0811", "walk@x"));

        assertEquals("0811", contact.noHp());
        assertEquals("walk@x", contact.email());
        assertFalse(contact.fromAccount());
    }

    @Test
    @DisplayName("a soft-deleted account no longer owns the contact")
    void deletedAccountIgnored() {
        CustomerAccountEntity deleted = account(7L, "0899", "acct@x");
        deleted.setDeletedAt(LocalDateTime.now());
        when(accountRepository.findByPelangganId(7L)).thenReturn(Optional.of(deleted));

        assertEquals("0811", resolver.resolve(pelanggan(7L, "0811", "old@x")).noHp());
        assertFalse(resolver.isManagedByAccount(7L));
    }

    @Test
    @DisplayName("applyTo fills a whole page with one lookup and blanks the deprecated landline")
    void applyToBatch() {
        TbPelangganEntity linked = pelanggan(7L, "0811", "old@x");
        TbPelangganEntity walkIn = pelanggan(8L, "0822", "walk@x");
        when(accountRepository.findByPelangganIds(anyCollection())).thenReturn(List.of(account(7L, "0899", "acct@x")));

        resolver.applyTo(List.of(linked, walkIn));

        assertEquals("0899", linked.getNoHp());
        assertEquals("acct@x", linked.getEmail());
        assertTrue(linked.isContactFromAccount());
        assertEquals("0822", walkIn.getNoHp());
        assertFalse(walkIn.isContactFromAccount());
        assertNull(linked.getNoTelepon());
        assertNull(walkIn.getTelepon());
        verify(accountRepository, times(1)).findByPelangganIds(anyCollection());
    }
}
