package com.github.b3kt.application.service.customer;

import com.github.b3kt.application.dto.customer.NopolCheckDto;
import com.github.b3kt.application.dto.customer.VehicleRegistrationResultDto;
import com.github.b3kt.application.dto.pazaauto.KendaraanAttachRequest;
import com.github.b3kt.application.dto.pazaauto.KendaraanOwnershipDto;
import com.github.b3kt.application.service.pazaauto.KendaraanOwnershipService;
import com.github.b3kt.application.service.pazaauto.PelangganMergeService;
import com.github.b3kt.domain.model.pazaauto.Pelanggan;
import com.github.b3kt.infrastructure.persistence.entity.customer.CustomerAccountEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPelangganEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPelangganKendaraanEntity;
import com.github.b3kt.infrastructure.persistence.repository.customer.CustomerAccountRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbPelangganKendaraanRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbPelangganRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CustomerVehicleService Tests")
class CustomerVehicleServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    CustomerAccountRepository accountRepository;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    TbPelangganRepository pelangganRepository;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    TbPelangganKendaraanRepository ownershipRepository;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    KendaraanOwnershipService ownershipService;

    @Mock
    PelangganMergeService mergeService;

    @InjectMocks
    CustomerVehicleService service;

    private CustomerAccountEntity account;
    /** The caller's own self-registered record, owning no vehicle yet. */
    private TbPelangganEntity portalRecord;
    private TbPelangganEntity onRecord;

    @BeforeEach
    void setUp() {
        account = new CustomerAccountEntity();
        account.setId(5L);
        account.setEmail("test@email.com");
        account.setFullName("Test User");
        account.setPhone("0812");

        portalRecord = new TbPelangganEntity();
        portalRecord.setId(2507L);
        portalRecord.setSumber(Pelanggan.SUMBER_PORTAL);
        portalRecord.setNamaPelanggan("Test User");

        onRecord = new TbPelangganEntity();
        onRecord.setId(2477L);
        onRecord.setNopol("B1567CFH");
        onRecord.setMerk("TOYOTA");
        onRecord.setJenis("AVANZA");
        onRecord.setNamaPelanggan("PAK SHOLEH");
    }

    /** Links the account to its own portal record, which owns no current vehicle. */
    private void linkedToPortalRecord() {
        account.setPelangganId(2507L);
        when(accountRepository.findByIdOptional(5L)).thenReturn(Optional.of(account));
        when(pelangganRepository.findByIdOptional(2507L)).thenReturn(Optional.of(portalRecord));
        when(ownershipRepository.findByPelangganIdCurrent(2507L)).thenReturn(List.of());
    }

    private TbPelangganKendaraanEntity ownership(Long id, Long pelangganId, String nopol, boolean current) {
        TbPelangganKendaraanEntity row = new TbPelangganKendaraanEntity();
        row.setId(id);
        row.setPelangganId(pelangganId);
        row.setNopol(nopol);
        row.setCurrent(current);
        return row;
    }

    private CustomerAccountEntity otherAccount(Long pelangganId) {
        CustomerAccountEntity other = new CustomerAccountEntity();
        other.setId(9L);
        other.setPelangganId(pelangganId);
        return other;
    }

    private KendaraanAttachRequest request(String nopol) {
        return KendaraanAttachRequest.builder().nopol(nopol).build();
    }

    private KendaraanOwnershipDto vehicleDto(String nopol, Long pelangganId) {
        KendaraanOwnershipDto dto = new KendaraanOwnershipDto();
        dto.setNopol(nopol);
        dto.setIdPelanggan(pelangganId);
        return dto;
    }

    @Nested
    @DisplayName("checkNopol")
    class CheckNopol {

        @Test
        @DisplayName("AVAILABLE when nothing matches")
        void available() {
            linkedToPortalRecord();
            when(ownershipRepository.findCurrentByNopol("B9999ZZ")).thenReturn(Optional.empty());

            NopolCheckDto result = service.checkNopol(5L, "b 9999 zz");

            assertEquals(NopolCheckDto.AVAILABLE, result.getStatus());
            assertEquals("B9999ZZ", result.getNopol());
            assertTrue(result.isCanRegister());
        }

        @Test
        @DisplayName("OWNED_BY_SELF when the caller's own record owns the plate")
        void ownedBySelf() {
            linkedToPortalRecord();
            when(ownershipRepository.findCurrentByNopol("B1567CFH"))
                    .thenReturn(Optional.of(ownership(2443L, 2507L, "B1567CFH", true)));

            assertEquals(NopolCheckDto.OWNED_BY_SELF, service.checkNopol(5L, "B1567CFH").getStatus());
        }

        @Test
        @DisplayName("OWNED_BY_OTHER when the plate belongs to another customer's account")
        void ownedByOtherClaimed() {
            linkedToPortalRecord();
            when(ownershipRepository.findCurrentByNopol("B1567CFH"))
                    .thenReturn(Optional.of(ownership(2443L, 999L, "B1567CFH", true)));
            when(accountRepository.findByPelangganId(999L)).thenReturn(Optional.of(otherAccount(999L)));

            assertEquals(NopolCheckDto.OWNED_BY_OTHER, service.checkNopol(5L, "B1567CFH").getStatus());
        }

        @Test
        @DisplayName("AVAILABLE when the plate is on an unclaimed on-record customer (adoptable)")
        void availableForAdoptable() {
            linkedToPortalRecord();
            when(ownershipRepository.findCurrentByNopol("B1567CFH"))
                    .thenReturn(Optional.of(ownership(2443L, 2477L, "B1567CFH", true)));
            when(accountRepository.findByPelangganId(2477L)).thenReturn(Optional.empty());

            assertTrue(service.checkNopol(5L, "B1567CFH").isCanRegister());
        }

        @Test
        @DisplayName("OWNED_BY_OTHER when a caller linked to a staff record matches another record's plate")
        void ownedByOtherWhenStaffRecord() {
            account.setPelangganId(2321L);
            when(accountRepository.findByIdOptional(5L)).thenReturn(Optional.of(account));
            when(pelangganRepository.findByIdOptional(2321L)).thenReturn(Optional.of(onRecord));
            when(ownershipRepository.findCurrentByNopol("B1567CFH"))
                    .thenReturn(Optional.of(ownership(2443L, 2477L, "B1567CFH", true)));
            when(accountRepository.findByPelangganId(2477L)).thenReturn(Optional.empty());

            assertEquals(NopolCheckDto.OWNED_BY_OTHER, service.checkNopol(5L, "B1567CFH").getStatus());
        }

        @Test
        @DisplayName("OWNED_BY_OTHER when the caller's portal record already owns a vehicle")
        void ownedByOtherWhenPortalRecordHasVehicle() {
            account.setPelangganId(2507L);
            when(accountRepository.findByIdOptional(5L)).thenReturn(Optional.of(account));
            when(pelangganRepository.findByIdOptional(2507L)).thenReturn(Optional.of(portalRecord));
            when(ownershipRepository.findByPelangganIdCurrent(2507L))
                    .thenReturn(List.of(ownership(1L, 2507L, "AB124CD", true)));
            when(ownershipRepository.findCurrentByNopol("B1567CFH"))
                    .thenReturn(Optional.of(ownership(2443L, 2477L, "B1567CFH", true)));
            when(accountRepository.findByPelangganId(2477L)).thenReturn(Optional.empty());

            assertEquals(NopolCheckDto.OWNED_BY_OTHER, service.checkNopol(5L, "B1567CFH").getStatus());
        }

        @Test
        @DisplayName("throws when the plate is blank")
        void blank() {
            when(accountRepository.findByIdOptional(5L)).thenReturn(Optional.of(account));
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.checkNopol(5L, "  "));
            assertTrue(ex.getMessage().contains("nopol is required"));
        }
    }

    @Nested
    @DisplayName("registerVehicle")
    class RegisterVehicle {

        @Test
        @DisplayName("adopts an unclaimed on-record customer and merges the caller's own record into it")
        void adoptsOnRecord() {
            linkedToPortalRecord();
            when(ownershipRepository.findCurrentByNopol("B1567CFH"))
                    .thenReturn(Optional.of(ownership(2443L, 2477L, "B1567CFH", true)));
            when(accountRepository.findByPelangganId(2477L)).thenReturn(Optional.empty());
            when(pelangganRepository.findByIdOptional(2477L)).thenReturn(Optional.of(onRecord));
            when(ownershipService.listCurrentVehiclesByPelanggan(2477L))
                    .thenReturn(List.of(vehicleDto("B1567CFH", 2477L)));

            VehicleRegistrationResultDto result = service.registerVehicle(5L, request("b1567 cfh"));

            assertTrue(result.isRelinked());
            assertEquals(2477L, account.getPelangganId());
            verify(mergeService).merge(2507L, 2477L);
            // The adopted record already has the current ownership row: no duplicate is opened.
            verify(ownershipService, never()).attach(any(), any(), any(), any(), any(), any());
            assertEquals(1, result.getVehicles().size());
        }

        @Test
        @DisplayName("adopts directly (no merge) when the account has no record of its own yet")
        void adoptsWithoutOwnRecord() {
            account.setPelangganId(null);
            when(accountRepository.findByIdOptional(5L)).thenReturn(Optional.of(account));
            when(ownershipRepository.findCurrentByNopol("B1567CFH"))
                    .thenReturn(Optional.of(ownership(2443L, 2477L, "B1567CFH", true)));
            when(accountRepository.findByPelangganId(2477L)).thenReturn(Optional.empty());
            when(pelangganRepository.findByIdOptional(2477L)).thenReturn(Optional.of(onRecord));

            VehicleRegistrationResultDto result = service.registerVehicle(5L, request("B1567CFH"));

            assertTrue(result.isRelinked());
            assertEquals(2477L, account.getPelangganId());
            verify(mergeService, never()).merge(any(), any());
        }

        @Test
        @DisplayName("reuses the caller's own portal record and opens ownership when the plate is new")
        void reusesPortalRecord() {
            linkedToPortalRecord();
            when(ownershipRepository.findCurrentByNopol("AB124CD")).thenReturn(Optional.empty());
            when(ownershipService.listCurrentVehiclesByPelanggan(2507L))
                    .thenReturn(List.of(vehicleDto("AB124CD", 2507L)));

            VehicleRegistrationResultDto result = service.registerVehicle(5L, request("AB124CD"));

            assertFalse(result.isRelinked());
            assertEquals(2507L, account.getPelangganId());
            // tb_pelanggan.nopol is derived from the ownership row by the DB trigger, never written here.
            assertNull(portalRecord.getNopol());
            assertNull(portalRecord.getDeletedAt());
            verify(ownershipService).attach(eq(2507L), eq("AB124CD"), isNull(), isNull(), isNull(), isNull());
            verify(mergeService, never()).merge(any(), any());
        }

        @Test
        @DisplayName("creates and links a portal-sourced record for an unlinked account")
        void createsForUnlinkedAccount() {
            account.setPelangganId(null);
            when(accountRepository.findByIdOptional(5L)).thenReturn(Optional.of(account));
            when(ownershipRepository.findCurrentByNopol("AB124CD")).thenReturn(Optional.empty());
            doAnswer(inv -> {
                TbPelangganEntity created = inv.getArgument(0);
                created.setId(9999L);
                return null;
            }).when(pelangganRepository).persist(any(TbPelangganEntity.class));
            when(ownershipService.listCurrentVehiclesByPelanggan(9999L))
                    .thenReturn(List.of(vehicleDto("AB124CD", 9999L)));

            VehicleRegistrationResultDto result = service.registerVehicle(5L, request("AB124CD"));

            ArgumentCaptor<TbPelangganEntity> captor = ArgumentCaptor.forClass(TbPelangganEntity.class);
            verify(pelangganRepository).persist(captor.capture());
            TbPelangganEntity created = captor.getValue();
            assertEquals(Pelanggan.SUMBER_PORTAL, created.getSumber());
            assertNull(created.getNopol());
            assertEquals("Test User", created.getNamaPelanggan());
            assertEquals("0812", created.getNoHp());
            assertEquals("test@email.com", created.getEmail());

            assertTrue(result.isRelinked());
            assertEquals(9999L, account.getPelangganId());
            verify(ownershipService).attach(eq(9999L), eq("AB124CD"), isNull(), isNull(), isNull(), isNull());
        }

        @Test
        @DisplayName("throws when the plate is already registered to the caller")
        void alreadyOwnedBySelf() {
            linkedToPortalRecord();
            when(ownershipRepository.findCurrentByNopol("B1567CFH"))
                    .thenReturn(Optional.of(ownership(2443L, 2507L, "B1567CFH", true)));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.registerVehicle(5L, request("B1567CFH")));
            assertTrue(ex.getMessage().contains("already registered to your account"));
            verify(ownershipService, never()).attach(any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("throws when the plate belongs to another customer's account")
        void ownedByOther() {
            linkedToPortalRecord();
            when(ownershipRepository.findCurrentByNopol("B1567CFH"))
                    .thenReturn(Optional.of(ownership(2443L, 999L, "B1567CFH", true)));
            when(accountRepository.findByPelangganId(999L)).thenReturn(Optional.of(otherAccount(999L)));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.registerVehicle(5L, request("B1567CFH")));
            assertTrue(ex.getMessage().contains("file a claim"));
            verify(mergeService, never()).merge(any(), any());
        }
    }
}
