package com.github.b3kt.application.service.customer;

import com.github.b3kt.application.dto.customer.VehicleClaimResultDto;
import com.github.b3kt.application.dto.pazaauto.KendaraanOwnershipDto;
import com.github.b3kt.application.service.pazaauto.KendaraanOwnershipService;
import com.github.b3kt.infrastructure.persistence.entity.customer.CustomerAccountEntity;
import com.github.b3kt.infrastructure.persistence.entity.customer.CustomerVehicleClaimRequestEntity;
import com.github.b3kt.infrastructure.persistence.repository.customer.CustomerAccountRepository;
import com.github.b3kt.infrastructure.persistence.repository.customer.CustomerVehicleClaimRequestRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbPelangganRepository;
import org.junit.jupiter.api.BeforeEach;
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

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("VehicleClaimService Tests")
class VehicleClaimServiceTest {

    @Mock
    CustomerAccountRepository accountRepository;

    @Mock
    CustomerVehicleClaimRequestRepository claimRequestRepository;

    @Mock
    TbPelangganRepository pelangganRepository;

    @Mock
    KendaraanOwnershipService kendaraanOwnershipService;

    @InjectMocks
    VehicleClaimService service;

    @BeforeEach
    void setUp() {
        service.claimMode = "APPROVAL";
    }

    private CustomerAccountEntity account(Long pelangganId) {
        CustomerAccountEntity account = new CustomerAccountEntity();
        account.setId(5L);
        account.setEmail("budi@test.com");
        account.setFullName("Budi");
        account.setPelangganId(pelangganId);
        return account;
    }

    private KendaraanOwnershipDto owner(Long pelangganId) {
        KendaraanOwnershipDto dto = new KendaraanOwnershipDto();
        dto.setIdPelanggan(pelangganId);
        dto.setNopol("B1234XYZ");
        dto.setNamaPelanggan("Jane");
        return dto;
    }

    @Nested
    @DisplayName("requestClaim (approval mode)")
    class ApprovalModeTests {

        @Test
        @DisplayName("queues a pending request when the plate is owned by another customer")
        void queuesPending() {
            when(accountRepository.findByIdOptional(5L)).thenReturn(Optional.of(account(7L)));
            when(kendaraanOwnershipService.getVehicleByNopol("B1234XYZ")).thenReturn(owner(9L));
            when(claimRequestRepository.findPendingByCustomerAccount(5L)).thenReturn(Optional.empty());
            when(claimRequestRepository.findPendingByNopol("B1234XYZ")).thenReturn(Optional.empty());

            VehicleClaimResultDto result = service.requestClaim(5L, "b 1234 xyz");

            assertEquals(VehicleClaimResultDto.STATUS_PENDING_APPROVAL, result.getStatus());
            ArgumentCaptor<CustomerVehicleClaimRequestEntity> captor =
                    ArgumentCaptor.forClass(CustomerVehicleClaimRequestEntity.class);
            verify(claimRequestRepository).persist(captor.capture());
            CustomerVehicleClaimRequestEntity saved = captor.getValue();
            assertEquals(5L, saved.getCustomerAccountId());
            assertEquals(7L, saved.getPelangganId());
            assertEquals("B1234XYZ", saved.getNopol());
            assertEquals(9L, saved.getCurrentOwnerPelangganId());
            verify(kendaraanOwnershipService, never()).transfer(any(), any(), any(), any());
        }

        @Test
        @DisplayName("throws when the current customer already owns the plate")
        void alreadyOwns() {
            when(accountRepository.findByIdOptional(5L)).thenReturn(Optional.of(account(7L)));
            when(kendaraanOwnershipService.getVehicleByNopol("B1234XYZ")).thenReturn(owner(7L));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.requestClaim(5L, "B1234XYZ"));
            assertTrue(ex.getMessage().contains("already registered to your account"));
        }

        @Test
        @DisplayName("throws when the plate is not registered to any customer")
        void notRegistered() {
            when(accountRepository.findByIdOptional(5L)).thenReturn(Optional.of(account(7L)));
            when(kendaraanOwnershipService.getVehicleByNopol("B1234XYZ")).thenReturn(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.requestClaim(5L, "B1234XYZ"));
            assertTrue(ex.getMessage().contains("not registered to any customer"));
        }

        @Test
        @DisplayName("throws when the account is not linked to a pelanggan")
        void notLinked() {
            when(accountRepository.findByIdOptional(5L)).thenReturn(Optional.of(account(null)));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.requestClaim(5L, "B1234XYZ"));
            assertTrue(ex.getMessage().contains("Link your account"));
        }

        @Test
        @DisplayName("throws when a pending request already exists for the account")
        void pendingExists() {
            when(accountRepository.findByIdOptional(5L)).thenReturn(Optional.of(account(7L)));
            when(kendaraanOwnershipService.getVehicleByNopol("B1234XYZ")).thenReturn(owner(9L));
            when(claimRequestRepository.findPendingByCustomerAccount(5L))
                    .thenReturn(Optional.of(new CustomerVehicleClaimRequestEntity()));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.requestClaim(5L, "B1234XYZ"));
            assertTrue(ex.getMessage().contains("pending vehicle claim request"));
        }
    }

    @Nested
    @DisplayName("requestClaim (immediate mode)")
    class ImmediateModeTests {

        @Test
        @DisplayName("transfers ownership right away")
        void transfersImmediately() {
            service.claimMode = "IMMEDIATE";
            when(accountRepository.findByIdOptional(5L)).thenReturn(Optional.of(account(7L)));
            when(kendaraanOwnershipService.getVehicleByNopol("B1234XYZ")).thenReturn(owner(9L));

            VehicleClaimResultDto result = service.requestClaim(5L, "B1234XYZ");

            assertEquals(VehicleClaimResultDto.STATUS_LINKED, result.getStatus());
            verify(kendaraanOwnershipService).transfer(eq("B1234XYZ"), eq(7L), any(LocalDate.class), anyString());
            verify(claimRequestRepository, never()).persist(any(CustomerVehicleClaimRequestEntity.class));
        }
    }

    @Nested
    @DisplayName("approve")
    class ApproveTests {

        private CustomerVehicleClaimRequestEntity pending() {
            CustomerVehicleClaimRequestEntity request = new CustomerVehicleClaimRequestEntity();
            request.setId(100L);
            request.setCustomerAccountId(5L);
            request.setPelangganId(7L);
            request.setNopol("B1234XYZ");
            request.setCurrentOwnerPelangganId(9L);
            return request;
        }

        @Test
        @DisplayName("transfers ownership from the current owner and marks APPROVED")
        void transfersFromCurrentOwner() {
            CustomerVehicleClaimRequestEntity request = pending();
            when(claimRequestRepository.findByIdOptional(100L)).thenReturn(Optional.of(request));
            when(kendaraanOwnershipService.getVehicleByNopol("B1234XYZ")).thenReturn(owner(9L));

            service.approve(100L, "staff");

            verify(kendaraanOwnershipService).transfer(eq("B1234XYZ"), eq(7L), any(LocalDate.class), anyString());
            assertEquals(CustomerVehicleClaimRequestEntity.STATUS_APPROVED, request.getStatus());
            assertEquals("staff", request.getReviewedBy());
            assertNotNull(request.getReviewedAt());
        }

        @Test
        @DisplayName("skips transfer when the plate is already owned by the claimant")
        void alreadyOwnedByClaimant() {
            CustomerVehicleClaimRequestEntity request = pending();
            when(claimRequestRepository.findByIdOptional(100L)).thenReturn(Optional.of(request));
            when(kendaraanOwnershipService.getVehicleByNopol("B1234XYZ")).thenReturn(owner(7L));

            service.approve(100L, "staff");

            verify(kendaraanOwnershipService, never()).transfer(any(), any(), any(), any());
            assertEquals(CustomerVehicleClaimRequestEntity.STATUS_APPROVED, request.getStatus());
        }

        @Test
        @DisplayName("throws when the request was already reviewed")
        void alreadyReviewed() {
            CustomerVehicleClaimRequestEntity request = pending();
            request.setStatus(CustomerVehicleClaimRequestEntity.STATUS_APPROVED);
            when(claimRequestRepository.findByIdOptional(100L)).thenReturn(Optional.of(request));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.approve(100L, "staff"));
            assertTrue(ex.getMessage().contains("already been reviewed"));
        }
    }

    @Nested
    @DisplayName("reject")
    class RejectTests {

        @Test
        @DisplayName("marks the request REJECTED with the review note")
        void rejectsWithNote() {
            CustomerVehicleClaimRequestEntity request = new CustomerVehicleClaimRequestEntity();
            request.setId(100L);
            when(claimRequestRepository.findByIdOptional(100L)).thenReturn(Optional.of(request));

            service.reject(100L, "staff", "identity not verified");

            assertEquals(CustomerVehicleClaimRequestEntity.STATUS_REJECTED, request.getStatus());
            assertEquals("staff", request.getReviewedBy());
            assertEquals("identity not verified", request.getReviewNote());
            assertNotNull(request.getReviewedAt());
        }
    }
}
