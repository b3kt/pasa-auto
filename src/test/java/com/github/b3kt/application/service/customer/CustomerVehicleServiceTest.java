package com.github.b3kt.application.service.customer;

import com.github.b3kt.application.dto.customer.NopolCheckDto;
import com.github.b3kt.application.dto.customer.VehicleRegistrationResultDto;
import com.github.b3kt.application.dto.customer.CustomerVehicleRegisterRequest;
import com.github.b3kt.application.dto.pazaauto.KendaraanOwnershipDto;
import com.github.b3kt.application.service.pazaauto.KendaraanOwnershipService;
import com.github.b3kt.application.service.pazaauto.PelangganMergeService;
import com.github.b3kt.domain.model.pazaauto.Pelanggan;
import com.github.b3kt.infrastructure.persistence.entity.customer.CustomerAccountEntity;
import com.github.b3kt.infrastructure.persistence.entity.customer.CustomerPelangganLinkRequestEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPelangganEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPelangganKendaraanEntity;
import com.github.b3kt.infrastructure.persistence.repository.customer.CustomerAccountRepository;
import com.github.b3kt.infrastructure.persistence.repository.customer.CustomerPelangganLinkRequestRepository;
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

    @Mock
    CustomerPelangganLinkRequestRepository linkRequestRepository;

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

    private CustomerVehicleRegisterRequest request(String nopol) {
        return CustomerVehicleRegisterRequest.builder().nopol(nopol).build();
    }

    /** Request answering the "is this record yours?" prompt. */
    private CustomerVehicleRegisterRequest request(String nopol, Boolean ownRecord) {
        return CustomerVehicleRegisterRequest.builder().nopol(nopol).ownRecord(ownRecord).build();
    }

    /** B1567CFH is currently owned by the unclaimed on-record customer 2477. */
    private void plateOnUnclaimedRecord() {
        when(ownershipRepository.findCurrentByNopol("B1567CFH"))
                .thenReturn(Optional.of(ownership(2443L, 2477L, "B1567CFH", true)));
        when(accountRepository.findByPelangganId(2477L)).thenReturn(Optional.empty());
        when(pelangganRepository.findByIdOptional(2477L)).thenReturn(Optional.of(onRecord));
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
        @DisplayName("ON_RECORD with brand, type and a masked owner name when the plate is on an unclaimed record")
        void onRecordForAdoptable() {
            linkedToPortalRecord();
            plateOnUnclaimedRecord();
            KendaraanOwnershipDto vehicle = vehicleDto("B1567CFH", 2477L);
            vehicle.setMerk("TOYOTA");
            vehicle.setJenis("AVANZA");
            vehicle.setNamaPelanggan("PAK SHOLEH");
            when(ownershipService.getVehicleByNopol("B1567CFH")).thenReturn(vehicle);

            NopolCheckDto result = service.checkNopol(5L, "B1567CFH");

            assertEquals(NopolCheckDto.ON_RECORD, result.getStatus());
            assertTrue(result.isCanRegister());
            assertEquals("TOYOTA", result.getMerk());
            assertEquals("AVANZA", result.getJenis());
            assertEquals("P********H", result.getMaskedOwnerName());
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
        @DisplayName("'yes, my record': links it and merges the caller's own record into it")
        void adoptsOnRecord() {
            linkedToPortalRecord();
            plateOnUnclaimedRecord();
            when(ownershipService.listCurrentVehiclesByPelanggan(2477L))
                    .thenReturn(List.of(vehicleDto("B1567CFH", 2477L)));

            VehicleRegistrationResultDto result = service.registerVehicle(5L, request("b1567 cfh", true));

            assertEquals(VehicleRegistrationResultDto.STATUS_REGISTERED, result.getStatus());
            assertTrue(result.isRelinked());
            assertEquals(2477L, account.getPelangganId());
            verify(mergeService).merge(2507L, 2477L);
            // The adopted record already has the current ownership row: no duplicate, no transfer.
            verify(ownershipService, never()).attach(any(), any(), any(), any(), any(), any());
            verify(ownershipService, never()).transfer(any(), any(), any(), any());
            assertEquals(1, result.getVehicles().size());
        }

        @Test
        @DisplayName("'yes': links directly (no merge) when the account has no record of its own yet")
        void adoptsWithoutOwnRecord() {
            account.setPelangganId(null);
            when(accountRepository.findByIdOptional(5L)).thenReturn(Optional.of(account));
            plateOnUnclaimedRecord();

            VehicleRegistrationResultDto result = service.registerVehicle(5L, request("B1567CFH", true));

            assertTrue(result.isRelinked());
            assertEquals(2477L, account.getPelangganId());
            verify(mergeService, never()).merge(any(), any());
        }

        @Test
        @DisplayName("'yes': applies the brand/type the customer corrected to the adopted vehicle")
        void adoptAppliesCorrections() {
            linkedToPortalRecord();
            plateOnUnclaimedRecord();
            CustomerVehicleRegisterRequest request = request("B1567CFH", true);
            request.setMerk("TOYOTA");
            request.setJenis("INNOVA");

            service.registerVehicle(5L, request);

            verify(ownershipService).updateVehicle(2477L, "B1567CFH", "TOYOTA", "INNOVA");
        }

        @Test
        @DisplayName("'yes' with a matching phone on record links immediately")
        void adoptWithMatchingContact() {
            linkedToPortalRecord();
            plateOnUnclaimedRecord();
            onRecord.setNoHp("+62 812");

            VehicleRegistrationResultDto result = service.registerVehicle(5L, request("B1567CFH", true));

            assertEquals(VehicleRegistrationResultDto.STATUS_REGISTERED, result.getStatus());
            assertEquals(2477L, account.getPelangganId());
        }

        @Test
        @DisplayName("'yes' but the record's contact contradicts the account: queued for staff approval")
        void adoptWithConflictingContactNeedsApproval() {
            linkedToPortalRecord();
            plateOnUnclaimedRecord();
            onRecord.setNoHp("08999000111");
            onRecord.setEmail("sholeh@example.com");
            when(linkRequestRepository.findPendingByCustomerAccount(5L)).thenReturn(Optional.empty());

            VehicleRegistrationResultDto result = service.registerVehicle(5L, request("B1567CFH", true));

            assertEquals(VehicleRegistrationResultDto.STATUS_PENDING_APPROVAL, result.getStatus());
            assertEquals(2507L, account.getPelangganId(), "account stays on its own record until approved");
            ArgumentCaptor<CustomerPelangganLinkRequestEntity> captor =
                    ArgumentCaptor.forClass(CustomerPelangganLinkRequestEntity.class);
            verify(linkRequestRepository).persist(captor.capture());
            assertEquals(2477L, captor.getValue().getPelangganId());
            assertEquals(CustomerPelangganLinkRequestEntity.MATCH_NOPOL, captor.getValue().getMatchType());
            assertEquals("B1567CFH", captor.getValue().getMatchValue());
            verify(mergeService, never()).merge(any(), any());
            verify(ownershipService, never()).transfer(any(), any(), any(), any());
        }

        @Test
        @DisplayName("'no, not my record': only the vehicle is transferred to the caller's own record")
        void notMyRecordTransfersVehicle() {
            linkedToPortalRecord();
            plateOnUnclaimedRecord();

            VehicleRegistrationResultDto result = service.registerVehicle(5L, request("B1567CFH", false));

            assertEquals(VehicleRegistrationResultDto.STATUS_REGISTERED, result.getStatus());
            assertFalse(result.isRelinked());
            assertEquals(2507L, account.getPelangganId(), "the old record is not linked");
            verify(ownershipService).transfer(eq("B1567CFH"), eq(2507L), any(), any());
            verify(mergeService, never()).merge(any(), any());
        }

        @Test
        @DisplayName("no answer and no own record yet: creates the caller's record, then transfers the vehicle")
        void noAnswerCreatesOwnRecordThenTransfers() {
            account.setPelangganId(null);
            when(accountRepository.findByIdOptional(5L)).thenReturn(Optional.of(account));
            plateOnUnclaimedRecord();
            doAnswer(inv -> {
                TbPelangganEntity created = inv.getArgument(0);
                created.setId(9999L);
                return null;
            }).when(pelangganRepository).persist(any(TbPelangganEntity.class));

            VehicleRegistrationResultDto result = service.registerVehicle(5L, request("B1567CFH"));

            assertTrue(result.isRelinked());
            assertEquals(9999L, account.getPelangganId());
            verify(ownershipService).transfer(eq("B1567CFH"), eq(9999L), any(), any());
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
            // Contacts stay on the account (ADR-0012); nothing is copied onto tb_pelanggan.
            assertNull(created.getNoHp());
            assertNull(created.getEmail());

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
