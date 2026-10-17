package com.github.b3kt.application.service.customer;

import com.github.b3kt.application.dto.customer.CustomerProfileDto;
import com.github.b3kt.application.dto.customer.UpdateProfileRequest;
import com.github.b3kt.domain.model.pazaauto.Pelanggan;
import com.github.b3kt.infrastructure.persistence.entity.customer.CustomerAccountEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPelangganEntity;
import com.github.b3kt.infrastructure.persistence.repository.customer.CustomerAccountRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbPelangganRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CustomerProfileService Tests")
class CustomerProfileServiceTest {

    @Mock
    CustomerAccountRepository accountRepository;

    @Mock
    TbPelangganRepository pelangganRepository;

    @InjectMocks
    CustomerProfileService service;

    private CustomerAccountEntity account(boolean linked) {
        CustomerAccountEntity account = new CustomerAccountEntity();
        account.setId(5L);
        account.setEmail("budi@test.com");
        account.setPhone("08123");
        account.setFullName("Budi");
        if (linked) {
            account.setPelangganId(7L);
        }
        return account;
    }

    private TbPelangganEntity pelanggan() {
        TbPelangganEntity p = new TbPelangganEntity();
        p.setId(7L);
        p.setNamaPelanggan("Budi Santoso");
        p.setAlamat("Jl. Merdeka 1");
        p.setTelepon("021");
        p.setNoHp("08123");
        p.setNoTelepon("021999");
        p.setEmail("budi@test.com");
        p.setKota("Bandung");
        p.setKodePos("40111");
        p.setContactPerson("Budi");
        return p;
    }

    @Nested
    @DisplayName("getProfile")
    class GetProfileTests {

        @Test
        @DisplayName("returns the account fields and, when linked, the pelanggan record")
        void linked() {
            when(accountRepository.findByIdOptional(5L)).thenReturn(Optional.of(account(true)));
            when(pelangganRepository.findByIdCached(7L)).thenReturn(Optional.of(pelanggan()));

            CustomerProfileDto dto = service.getProfile(5L);

            assertEquals(5L, dto.getAccountId());
            assertEquals("budi@test.com", dto.getEmail());
            assertEquals("Budi Santoso", dto.getFullName(), "linked record's name is authoritative");
            assertTrue(dto.isLinked());
            assertEquals(7L, dto.getPelangganId());
            assertEquals("Budi Santoso", dto.getNamaPelanggan());
            assertEquals("Jl. Merdeka 1", dto.getAlamat());
            // telepon is the deprecated alias of noTelepon (ADR-0011), not the legacy tlpn value
            assertEquals("021999", dto.getTelepon());
            assertEquals("08123", dto.getNoHp());
            assertEquals("021999", dto.getNoTelepon());
            assertEquals("budi@test.com", dto.getPelangganEmail());
            assertEquals("Bandung", dto.getKota());
            assertEquals("40111", dto.getKodePos());
            assertEquals("Budi", dto.getContactPerson());
        }

        @Test
        @DisplayName("returns only the account fields when the account is not linked")
        void notLinked() {
            when(accountRepository.findByIdOptional(5L)).thenReturn(Optional.of(account(false)));

            CustomerProfileDto dto = service.getProfile(5L);

            assertFalse(dto.isLinked());
            assertNull(dto.getPelangganId());
            assertEquals("Budi", dto.getFullName());
            verify(pelangganRepository, never()).findByIdCached(any());
        }

        @Test
        @DisplayName("tolerates a linked account whose pelanggan record is missing")
        void linkedButRecordMissing() {
            when(accountRepository.findByIdOptional(5L)).thenReturn(Optional.of(account(true)));
            when(pelangganRepository.findByIdCached(7L)).thenReturn(Optional.empty());

            CustomerProfileDto dto = service.getProfile(5L);

            assertTrue(dto.isLinked());
            assertNull(dto.getNamaPelanggan());
        }

        @Test
        @DisplayName("rejects an unknown account")
        void unknown() {
            when(accountRepository.findByIdOptional(5L)).thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.getProfile(5L));
            assertEquals("Account not found", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("updateProfile")
    class UpdateProfileTests {

        private UpdateProfileRequest request() {
            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setNamaPelanggan("  Budi Santoso  ");
            request.setAlamat("Jl. Baru 2");
            request.setTelepon("022");
            request.setNoHp("08999");
            request.setNoTelepon("022888");
            request.setEmail("baru@test.com");
            request.setKota("Jakarta");
            request.setKodePos("12345");
            request.setContactPerson("Ani");
            return request;
        }

        @Test
        @DisplayName("copies every editable field onto the linked record and invalidates the cache")
        void success() {
            TbPelangganEntity p = pelanggan();
            when(accountRepository.findByIdOptional(5L)).thenReturn(Optional.of(account(true)));
            when(pelangganRepository.findByIdOptional(7L)).thenReturn(Optional.of(p));

            service.updateProfile(5L, request());

            assertEquals("Budi Santoso", p.getNamaPelanggan());
            assertEquals("Jl. Baru 2", p.getAlamat());
            assertEquals("022", p.getTelepon());
            assertEquals("08999", p.getNoHp());
            assertEquals("022888", p.getNoTelepon());
            assertEquals("baru@test.com", p.getEmail());
            assertEquals("Jakarta", p.getKota());
            assertEquals("12345", p.getKodePos());
            assertEquals("Ani", p.getContactPerson());
            verify(pelangganRepository).invalidatePelangganCache();
        }

        @Test
        @DisplayName("creates and links a portal-sourced record without a vehicle when the account is not linked")
        void createsWhenUnlinked() {
            CustomerAccountEntity account = account(false);
            when(accountRepository.findByIdOptional(5L)).thenReturn(Optional.of(account));
            doAnswer(invocation -> {
                TbPelangganEntity entity = invocation.getArgument(0);
                entity.setId(99L);
                return null;
            }).when(pelangganRepository).persist(any(TbPelangganEntity.class));

            service.updateProfile(5L, request());

            ArgumentCaptor<TbPelangganEntity> captor = ArgumentCaptor.forClass(TbPelangganEntity.class);
            verify(pelangganRepository).persist(captor.capture());
            TbPelangganEntity created = captor.getValue();
            assertNull(created.getNopol());
            assertNull(created.getMerk());
            assertEquals(Pelanggan.SUMBER_PORTAL, created.getSumber());
            assertEquals("baru@test.com", created.getEmail(), "the request's email overrides the seeded one");
            assertEquals("Budi Santoso", created.getNamaPelanggan());
            assertEquals(99L, account.getPelangganId());
            assertNotNull(account.getLinkedAt());
            verify(pelangganRepository).invalidatePelangganCache();
        }

        @Test
        @DisplayName("creates and relinks when the linked record no longer exists")
        void recreatesWhenLinkedRecordMissing() {
            CustomerAccountEntity account = account(true);
            when(accountRepository.findByIdOptional(5L)).thenReturn(Optional.of(account));
            when(pelangganRepository.findByIdOptional(7L)).thenReturn(Optional.empty());
            doAnswer(invocation -> {
                TbPelangganEntity entity = invocation.getArgument(0);
                entity.setId(99L);
                return null;
            }).when(pelangganRepository).persist(any(TbPelangganEntity.class));

            service.updateProfile(5L, request());

            assertEquals(99L, account.getPelangganId());
            verify(pelangganRepository).persist(any(TbPelangganEntity.class));
        }

        @Test
        @DisplayName("rejects an unknown account")
        void unknown() {
            when(accountRepository.findByIdOptional(5L)).thenReturn(Optional.empty());

            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setNamaPelanggan("Budi");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.updateProfile(5L, request));
            assertEquals("Account not found", ex.getMessage());
            verify(pelangganRepository, never()).invalidatePelangganCache();
        }
    }
}
