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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PelangganMergeServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    TbPelangganRepository pelangganRepository;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    TbPelangganKendaraanRepository ownershipRepository;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    CustomerAccountRepository accountRepository;

    @Mock
    CustomerVehicleClaimRequestRepository claimRequestRepository;

    @Mock
    CustomerPelangganLinkRequestRepository linkRequestRepository;

    @Mock
    JpaPelangganRepository pelangganNopolRepository;

    @InjectMocks
    PelangganMergeService service;

    private TbPelangganEntity duplicate;
    private TbPelangganEntity survivor;

    @BeforeEach
    void setUp() {
        duplicate = new TbPelangganEntity();
        duplicate.setId(2507L);
        survivor = new TbPelangganEntity();
        survivor.setId(2477L);
        when(pelangganRepository.findByIdOptional(2507L)).thenReturn(Optional.of(duplicate));
        when(pelangganRepository.findByIdOptional(2477L)).thenReturn(Optional.of(survivor));
        when(accountRepository.findByPelangganId(any())).thenReturn(Optional.empty());
        when(ownershipRepository.findByPelangganId(2507L)).thenReturn(List.of());
    }

    @Test
    @DisplayName("moves ownership history, repoints pending requests and soft-deletes with a merge pointer")
    void merges() {
        TbPelangganKendaraanEntity history = new TbPelangganKendaraanEntity();
        history.setPelangganId(2507L);
        history.setNopol("B4444DD");
        when(ownershipRepository.findByPelangganId(2507L)).thenReturn(List.of(history));

        service.merge(2507L, 2477L);

        assertEquals(2477L, history.getPelangganId());
        verify(pelangganNopolRepository).invalidateByNopol("B4444DD");
        verify(claimRequestRepository).update("pelangganId = ?1 where pelangganId = ?2 and status = ?3",
                2477L, 2507L, CustomerVehicleClaimRequestEntity.STATUS_PENDING);
        verify(linkRequestRepository).update("pelangganId = ?1 where pelangganId = ?2 and status = ?3",
                2477L, 2507L, CustomerPelangganLinkRequestEntity.STATUS_PENDING);
        assertEquals(2477L, duplicate.getMergedIntoId());
        assertNotNull(duplicate.getDeletedAt());
        assertNull(survivor.getDeletedAt());
        verify(pelangganRepository).invalidatePelangganCache();
    }

    @Test
    @DisplayName("moves the duplicate's portal account link to the survivor")
    void movesAccount() {
        CustomerAccountEntity account = new CustomerAccountEntity();
        account.setPelangganId(2507L);
        when(accountRepository.findByPelangganId(2507L)).thenReturn(Optional.of(account));

        service.merge(2507L, 2477L);

        assertEquals(2477L, account.getPelangganId());
        assertNotNull(account.getLinkedAt());
    }

    @Test
    @DisplayName("refuses when both records are linked to accounts (strict 1:1)")
    void bothLinked() {
        when(accountRepository.findByPelangganId(2507L)).thenReturn(Optional.of(new CustomerAccountEntity()));
        when(accountRepository.findByPelangganId(2477L)).thenReturn(Optional.of(new CustomerAccountEntity()));

        assertThrows(IllegalArgumentException.class, () -> service.merge(2507L, 2477L));
        assertNull(duplicate.getDeletedAt());
    }

    @Test
    @DisplayName("refuses to merge a record into itself or into a deleted record")
    void invalidTargets() {
        assertThrows(IllegalArgumentException.class, () -> service.merge(2507L, 2507L));

        survivor.setDeletedAt(java.time.LocalDateTime.now());
        assertThrows(IllegalArgumentException.class, () -> service.merge(2507L, 2477L));
        assertNull(duplicate.getDeletedAt());
    }
}
