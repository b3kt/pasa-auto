package com.github.b3kt.application.service.customer;

import com.github.b3kt.application.dto.customer.LinkAccountRequest;
import com.github.b3kt.application.dto.customer.LinkRequestAdminDto;
import com.github.b3kt.application.dto.customer.LinkRequestResultDto;
import com.github.b3kt.application.dto.customer.LinkSearchResultDto;
import com.github.b3kt.application.dto.pazaauto.KendaraanOwnershipDto;
import com.github.b3kt.application.service.pazaauto.KendaraanOwnershipService;
import com.github.b3kt.application.service.pazaauto.PelangganMergeService;
import com.github.b3kt.domain.model.pazaauto.Pelanggan;
import com.github.b3kt.infrastructure.persistence.entity.customer.CustomerAccountEntity;
import com.github.b3kt.infrastructure.persistence.entity.customer.CustomerPelangganLinkRequestEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPelangganEntity;
import com.github.b3kt.infrastructure.persistence.repository.customer.CustomerAccountRepository;
import com.github.b3kt.infrastructure.persistence.repository.customer.CustomerPelangganLinkRequestRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CustomerLinkService Tests")
class CustomerLinkServiceTest {

    @Mock
    CustomerAccountRepository accountRepository;

    @Mock
    CustomerPelangganLinkRequestRepository linkRequestRepository;

    @Mock
    TbPelangganRepository pelangganRepository;

    @Mock
    KendaraanOwnershipService kendaraanOwnershipService;

    @Mock
    PelangganMergeService mergeService;

    @InjectMocks
    CustomerLinkService service;

    private CustomerAccountEntity account(Long pelangganId) {
        CustomerAccountEntity account = new CustomerAccountEntity();
        account.setId(5L);
        account.setEmail("budi@test.com");
        account.setFullName("Budi");
        account.setPelangganId(pelangganId);
        return account;
    }

    private TbPelangganEntity pelanggan() {
        TbPelangganEntity p = new TbPelangganEntity();
        p.setId(7L);
        p.setNamaPelanggan("Budi Santoso");
        p.setNopol("B1234XYZ");
        p.setNoHp("08123");
        p.setEmail("budi@test.com");
        return p;
    }

    @Nested
    @DisplayName("searchByNopol")
    class SearchByNopolTests {

        @Test
        @DisplayName("returns a masked match for an owned vehicle")
        void found() {
            KendaraanOwnershipDto owner = new KendaraanOwnershipDto();
            owner.setIdPelanggan(7L);
            owner.setNopol("B1234XYZ");
            owner.setNamaPelanggan("Budi Santoso");
            owner.setNoHpPelanggan("08123");
            when(kendaraanOwnershipService.getVehicleByNopol("B1234XYZ")).thenReturn(owner);

            LinkSearchResultDto dto = service.searchByNopol("B1234XYZ");

            assertNotNull(dto);
            assertEquals(7L, dto.getPelangganId());
            assertEquals("B1234XYZ", dto.getNopol());
            assertEquals("B**********o", dto.getMaskedName());
            assertEquals("**123", dto.getMaskedPhone());
        }

        @Test
        @DisplayName("returns null when the vehicle is unknown")
        void unknownVehicle() {
            when(kendaraanOwnershipService.getVehicleByNopol("B0000")).thenReturn(null);

            assertNull(service.searchByNopol("B0000"));
        }

        @Test
        @DisplayName("returns null when the vehicle has no owner on record")
        void noOwner() {
            KendaraanOwnershipDto owner = new KendaraanOwnershipDto();
            owner.setNopol("B0000");
            when(kendaraanOwnershipService.getVehicleByNopol("B0000")).thenReturn(owner);

            assertNull(service.searchByNopol("B0000"));
        }
    }

    @Nested
    @DisplayName("searchByContact")
    class SearchByContactTests {

        @Test
        @DisplayName("maps every match with masked fields (mobile only, never the deprecated landline)")
        void found() {
            TbPelangganEntity p = pelanggan();
            p.setNoHp("0812345");
            p.setNoTelepon("0219999");
            when(pelangganRepository.findByContact("0812345", "budi@test.com")).thenReturn(List.of(p));

            List<LinkSearchResultDto> results = service.searchByContact("0812345", "budi@test.com");

            assertEquals(1, results.size());
            LinkSearchResultDto dto = results.get(0);
            assertEquals(7L, dto.getPelangganId());
            assertEquals("B**********o", dto.getMaskedName());
            assertEquals("****345", dto.getMaskedPhone());
            assertEquals("**********com", dto.getMaskedEmail());
        }

        @Test
        @DisplayName("returns an empty list when there is no match")
        void none() {
            when(pelangganRepository.findByContact(any(), any())).thenReturn(List.of());

            assertTrue(service.searchByContact("x", "y").isEmpty());
        }
    }

    @Nested
    @DisplayName("requestLink")
    class RequestLinkTests {

        private LinkAccountRequest request() {
            LinkAccountRequest request = new LinkAccountRequest();
            request.setPelangganId(7L);
            request.setMatchType(CustomerPelangganLinkRequestEntity.MATCH_NOPOL);
            request.setMatchValue("B1234XYZ");
            return request;
        }

        @Test
        @DisplayName("auto-links when the pelanggan record is not claimed")
        void autoLink() {
            CustomerAccountEntity account = account(null);
            when(accountRepository.findByIdOptional(5L)).thenReturn(Optional.of(account));
            when(pelangganRepository.findByIdOptional(7L)).thenReturn(Optional.of(pelanggan()));
            when(accountRepository.findByPelangganId(7L)).thenReturn(Optional.empty());

            LinkRequestResultDto result = service.requestLink(5L, request());

            assertEquals(LinkRequestResultDto.STATUS_LINKED, result.getStatus());
            assertEquals("Your account is now linked to Budi Santoso", result.getMessage());
            assertEquals(7L, account.getPelangganId());
            assertNotNull(account.getLinkedAt());
        }

        @Test
        @DisplayName("queues for approval when the pelanggan record is already claimed")
        void pendingApproval() {
            when(accountRepository.findByIdOptional(5L)).thenReturn(Optional.of(account(null)));
            when(pelangganRepository.findByIdOptional(7L)).thenReturn(Optional.of(pelanggan()));
            when(accountRepository.findByPelangganId(7L)).thenReturn(Optional.of(account(7L)));
            when(linkRequestRepository.findPendingByCustomerAccount(5L)).thenReturn(Optional.empty());

            LinkRequestResultDto result = service.requestLink(5L, request());

            assertEquals(LinkRequestResultDto.STATUS_PENDING_APPROVAL, result.getStatus());
            ArgumentCaptor<CustomerPelangganLinkRequestEntity> captor =
                    ArgumentCaptor.forClass(CustomerPelangganLinkRequestEntity.class);
            verify(linkRequestRepository).persist(captor.capture());
            assertEquals(5L, captor.getValue().getCustomerAccountId());
            assertEquals(7L, captor.getValue().getPelangganId());
            assertEquals(CustomerPelangganLinkRequestEntity.STATUS_PENDING, captor.getValue().getStatus());
        }

        @Test
        @DisplayName("rejects an unknown account")
        void unknownAccount() {
            when(accountRepository.findByIdOptional(5L)).thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.requestLink(5L, request()));
            assertEquals("Account not found", ex.getMessage());
        }

        @Test
        @DisplayName("rejects an account that is already linked")
        void alreadyLinked() {
            when(accountRepository.findByIdOptional(5L)).thenReturn(Optional.of(account(9L)));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.requestLink(5L, request()));
            assertEquals("Account is already linked to a customer record", ex.getMessage());
        }

        @Test
        @DisplayName("rejects an unknown pelanggan record")
        void unknownPelanggan() {
            when(accountRepository.findByIdOptional(5L)).thenReturn(Optional.of(account(null)));
            when(pelangganRepository.findByIdOptional(7L)).thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.requestLink(5L, request()));
            assertEquals("Pelanggan not found", ex.getMessage());
        }

        @Test
        @DisplayName("rejects a second pending request")
        void duplicatePending() {
            when(accountRepository.findByIdOptional(5L)).thenReturn(Optional.of(account(null)));
            when(pelangganRepository.findByIdOptional(7L)).thenReturn(Optional.of(pelanggan()));
            when(accountRepository.findByPelangganId(7L)).thenReturn(Optional.of(account(7L)));
            when(linkRequestRepository.findPendingByCustomerAccount(5L))
                    .thenReturn(Optional.of(new CustomerPelangganLinkRequestEntity()));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.requestLink(5L, request()));
            assertEquals("You already have a pending link request", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("listRequests")
    class ListRequestsTests {

        @Test
        @DisplayName("maps each request with the joined account and pelanggan details")
        void maps() {
            CustomerPelangganLinkRequestEntity request = new CustomerPelangganLinkRequestEntity();
            request.setId(11L);
            request.setCustomerAccountId(5L);
            request.setPelangganId(7L);
            request.setMatchType(CustomerPelangganLinkRequestEntity.MATCH_CONTACT);
            request.setMatchValue("08123");
            request.setStatus(CustomerPelangganLinkRequestEntity.STATUS_PENDING);
            request.setCreatedAt(LocalDateTime.now());
            when(linkRequestRepository.findByStatus("PENDING")).thenReturn(List.of(request));
            when(accountRepository.findByIdOptional(5L)).thenReturn(Optional.of(account(null)));
            when(pelangganRepository.findByIdCached(7L)).thenReturn(Optional.of(pelanggan()));

            List<LinkRequestAdminDto> dtos = service.listRequests("PENDING");

            assertEquals(1, dtos.size());
            LinkRequestAdminDto dto = dtos.get(0);
            assertEquals(11L, dto.getId());
            assertEquals("budi@test.com", dto.getCustomerEmail());
            assertEquals("Budi", dto.getCustomerFullName());
            assertEquals("Budi Santoso", dto.getNamaPelanggan());
        }

        @Test
        @DisplayName("tolerates requests whose account or pelanggan was removed")
        void missingRelations() {
            CustomerPelangganLinkRequestEntity request = new CustomerPelangganLinkRequestEntity();
            request.setId(11L);
            request.setCustomerAccountId(5L);
            request.setPelangganId(7L);
            when(linkRequestRepository.findByStatus(null)).thenReturn(List.of(request));
            when(accountRepository.findByIdOptional(5L)).thenReturn(Optional.empty());
            when(pelangganRepository.findByIdCached(7L)).thenReturn(Optional.empty());

            LinkRequestAdminDto dto = service.listRequests(null).get(0);

            assertNull(dto.getCustomerEmail());
            assertNull(dto.getNamaPelanggan());
        }
    }

    @Nested
    @DisplayName("approve")
    class ApproveTests {

        @Test
        @DisplayName("moves the link from the previous holder to the requester")
        void success() {
            CustomerPelangganLinkRequestEntity request = new CustomerPelangganLinkRequestEntity();
            request.setId(11L);
            request.setCustomerAccountId(5L);
            request.setPelangganId(7L);
            request.setStatus(CustomerPelangganLinkRequestEntity.STATUS_PENDING);
            when(linkRequestRepository.findByIdOptional(11L)).thenReturn(Optional.of(request));
            CustomerAccountEntity previousHolder = account(7L);
            when(accountRepository.findByPelangganId(7L)).thenReturn(Optional.of(previousHolder));
            CustomerAccountEntity requester = account(null);
            when(accountRepository.findByIdOptional(5L)).thenReturn(Optional.of(requester));

            service.approve(11L, "admin");

            assertNull(previousHolder.getPelangganId());
            assertEquals(7L, requester.getPelangganId());
            assertNotNull(requester.getLinkedAt());
            assertEquals(CustomerPelangganLinkRequestEntity.STATUS_APPROVED, request.getStatus());
            assertEquals("admin", request.getReviewedBy());
            assertNotNull(request.getReviewedAt());
        }

        @Test
        @DisplayName("approves when the pelanggan record was not previously claimed")
        void noPreviousHolder() {
            CustomerPelangganLinkRequestEntity request = new CustomerPelangganLinkRequestEntity();
            request.setId(11L);
            request.setCustomerAccountId(5L);
            request.setPelangganId(7L);
            request.setStatus(CustomerPelangganLinkRequestEntity.STATUS_PENDING);
            when(linkRequestRepository.findByIdOptional(11L)).thenReturn(Optional.of(request));
            when(accountRepository.findByPelangganId(7L)).thenReturn(Optional.empty());
            when(accountRepository.findByIdOptional(5L)).thenReturn(Optional.of(account(null)));

            service.approve(11L, "admin");

            assertEquals(CustomerPelangganLinkRequestEntity.STATUS_APPROVED, request.getStatus());
        }

        @Test
        @DisplayName("merges the requester's own empty portal record into the approved record")
        void foldsOwnEmptyPortalRecord() {
            CustomerPelangganLinkRequestEntity request = new CustomerPelangganLinkRequestEntity();
            request.setId(11L);
            request.setCustomerAccountId(5L);
            request.setPelangganId(7L);
            request.setStatus(CustomerPelangganLinkRequestEntity.STATUS_PENDING);
            TbPelangganEntity own = new TbPelangganEntity();
            own.setId(99L);
            own.setSumber(Pelanggan.SUMBER_PORTAL);
            when(linkRequestRepository.findByIdOptional(11L)).thenReturn(Optional.of(request));
            when(accountRepository.findByPelangganId(7L)).thenReturn(Optional.empty());
            when(accountRepository.findByIdOptional(5L)).thenReturn(Optional.of(account(99L)));
            when(pelangganRepository.findByIdOptional(99L)).thenReturn(Optional.of(own));
            when(kendaraanOwnershipService.listCurrentVehiclesByPelanggan(99L)).thenReturn(List.of());

            service.approve(11L, "admin");

            verify(mergeService).merge(99L, 7L);
        }

        @Test
        @DisplayName("does not merge a requester's own record that is a staff record")
        void keepsOwnStaffRecord() {
            CustomerPelangganLinkRequestEntity request = new CustomerPelangganLinkRequestEntity();
            request.setId(11L);
            request.setCustomerAccountId(5L);
            request.setPelangganId(7L);
            request.setStatus(CustomerPelangganLinkRequestEntity.STATUS_PENDING);
            TbPelangganEntity staffRecord = new TbPelangganEntity();
            staffRecord.setId(99L);
            when(linkRequestRepository.findByIdOptional(11L)).thenReturn(Optional.of(request));
            when(accountRepository.findByPelangganId(7L)).thenReturn(Optional.empty());
            when(accountRepository.findByIdOptional(5L)).thenReturn(Optional.of(account(99L)));
            when(pelangganRepository.findByIdOptional(99L)).thenReturn(Optional.of(staffRecord));

            service.approve(11L, "admin");

            verify(mergeService, never()).merge(any(), any());
        }

        @Test
        @DisplayName("rejects an unknown request")
        void unknown() {
            when(linkRequestRepository.findByIdOptional(11L)).thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.approve(11L, "admin"));
            assertEquals("Link request not found", ex.getMessage());
        }

        @Test
        @DisplayName("rejects a request that was already reviewed")
        void alreadyReviewed() {
            CustomerPelangganLinkRequestEntity request = new CustomerPelangganLinkRequestEntity();
            request.setStatus(CustomerPelangganLinkRequestEntity.STATUS_APPROVED);
            when(linkRequestRepository.findByIdOptional(11L)).thenReturn(Optional.of(request));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.approve(11L, "admin"));
            assertEquals("This request has already been reviewed", ex.getMessage());
        }

        @Test
        @DisplayName("rejects when the requester account vanished")
        void requesterMissing() {
            CustomerPelangganLinkRequestEntity request = new CustomerPelangganLinkRequestEntity();
            request.setId(11L);
            request.setCustomerAccountId(5L);
            request.setPelangganId(7L);
            request.setStatus(CustomerPelangganLinkRequestEntity.STATUS_PENDING);
            when(linkRequestRepository.findByIdOptional(11L)).thenReturn(Optional.of(request));
            when(accountRepository.findByPelangganId(7L)).thenReturn(Optional.empty());
            when(accountRepository.findByIdOptional(5L)).thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.approve(11L, "admin"));
            assertEquals("Account not found", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("reject")
    class RejectTests {

        @Test
        @DisplayName("records the reviewer and the note")
        void success() {
            CustomerPelangganLinkRequestEntity request = new CustomerPelangganLinkRequestEntity();
            request.setStatus(CustomerPelangganLinkRequestEntity.STATUS_PENDING);
            when(linkRequestRepository.findByIdOptional(11L)).thenReturn(Optional.of(request));

            service.reject(11L, "admin", "identity not confirmed");

            assertEquals(CustomerPelangganLinkRequestEntity.STATUS_REJECTED, request.getStatus());
            assertEquals("admin", request.getReviewedBy());
            assertEquals("identity not confirmed", request.getReviewNote());
            assertNotNull(request.getReviewedAt());
        }

        @Test
        @DisplayName("rejects an unknown request")
        void unknown() {
            when(linkRequestRepository.findByIdOptional(11L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> service.reject(11L, "admin", "n"));
        }
    }
}
