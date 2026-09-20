package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbKendaraanEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPelangganKendaraanEntity;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbPelangganKendaraanRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PelangganOwnershipSyncService Tests")
class PelangganOwnershipSyncServiceTest {

    @Mock
    TbKendaraanService kendaraanService;

    @Mock
    TbPelangganKendaraanRepository ownershipRepository;

    @InjectMocks
    PelangganOwnershipSyncService service;

    private TbKendaraanEntity toyotaSUV;

    @BeforeEach
    void setUp() {
        toyotaSUV = new TbKendaraanEntity();
        toyotaSUV.setId(10L);
        toyotaSUV.setMerk("Toyota");
        toyotaSUV.setJenis("SUV");
    }

    // ---------- syncOnCreate ----------

    @Test
    @DisplayName("syncOnCreate skips when pelangganId is null")
    void testSyncOnCreate_nullPelangganId() {
        service.syncOnCreate(null, "B1234CD", "Toyota", "SUV", LocalDate.now());

        verifyNoInteractions(ownershipRepository);
        verifyNoInteractions(kendaraanService);
    }

    @Test
    @DisplayName("syncOnCreate skips when nopol is blank")
    void testSyncOnCreate_blankNopol() {
        service.syncOnCreate(1L, "   ", "Toyota", "SUV", LocalDate.now());

        verifyNoInteractions(ownershipRepository);
        verifyNoInteractions(kendaraanService);
    }

    @Test
    @DisplayName("syncOnCreate skips when nopol already owned")
    void testSyncOnCreate_existingOwnershipWins() {
        when(ownershipRepository.findCurrentByNopol("B1234CD")).thenReturn(Optional.of(new TbPelangganKendaraanEntity()));

        service.syncOnCreate(1L, "B1234CD", "Toyota", "SUV", LocalDate.now());

        verify(ownershipRepository).findCurrentByNopol("B1234CD");
        verifyNoInteractions(kendaraanService);
        verify(ownershipRepository, never()).openOwnership(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("syncOnCreate opens ownership and reuses master")
    void testSyncOnCreate_success() {
        when(ownershipRepository.findCurrentByNopol("B1234CD")).thenReturn(Optional.empty());
        when(kendaraanService.findOrCreateByMerkJenis("Toyota", "SUV")).thenReturn(toyotaSUV);
        LocalDate tanggal = LocalDate.of(2026, 5, 1);

        service.syncOnCreate(1L, "B1234CD", "Toyota", "SUV", tanggal);

        verify(kendaraanService).findOrCreateByMerkJenis("Toyota", "SUV");
        verify(ownershipRepository).openOwnership(1L, 10L, "B1234CD", tanggal, null);
    }

    @Test
    @DisplayName("syncOnCreate defaults tanggalMulai to today")
    void testSyncOnCreate_defaultTanggal() {
        when(ownershipRepository.findCurrentByNopol("B1234CD")).thenReturn(Optional.empty());
        when(kendaraanService.findOrCreateByMerkJenis("Toyota", "SUV")).thenReturn(toyotaSUV);

        service.syncOnCreate(1L, "B1234CD", "Toyota", "SUV", null);

        verify(ownershipRepository).openOwnership(eq(1L), eq(10L), eq("B1234CD"), any(LocalDate.class), isNull());
    }

    // ---------- syncOnUpdate ----------

    @Test
    @DisplayName("syncOnUpdate skips when pelangganId is null")
    void testSyncOnUpdate_nullPelangganId() {
        service.syncOnUpdate(null, "B1234CD", "Toyota", "SUV", "B5678EF", "Honda", "Sedan", LocalDate.now());

        verifyNoInteractions(ownershipRepository);
        verifyNoInteractions(kendaraanService);
    }

    @Test
    @DisplayName("syncOnUpdate with nopol change closes old and opens new ownership")
    void testSyncOnUpdate_nopolChanged() {
        LocalDate today = LocalDate.now();
        TbPelangganKendaraanEntity oldRow = new TbPelangganKendaraanEntity();
        oldRow.setId(100L);
        oldRow.setPelangganId(1L);
        oldRow.setNopol("B1234CD");
        oldRow.setTanggalMulai(LocalDate.now().minusDays(30));
        oldRow.setCurrent(true);

        when(ownershipRepository.findCurrentByNopolAndPelanggan("B1234CD", 1L)).thenReturn(Optional.of(oldRow));
        when(ownershipRepository.findCurrentByNopol("B5678EF")).thenReturn(Optional.empty());
        when(kendaraanService.findOrCreateByMerkJenis("Honda", "Sedan")).thenReturn(toyotaSUV);

        service.syncOnUpdate(1L, "B1234CD", "Toyota", "SUV", "B5678EF", "Honda", "Sedan", today);

        verify(ownershipRepository).closeOwnership(100L, today.minusDays(1), "nopol berubah");
        verify(ownershipRepository).openOwnership(1L, 10L, "B5678EF", today, null);
    }

    @Test
    @DisplayName("syncOnUpdate does not open a new row when the new nopol is taken elsewhere")
    void testSyncOnUpdate_nopolTaken() {
        LocalDate today = LocalDate.now();
        TbPelangganKendaraanEntity oldRow = new TbPelangganKendaraanEntity();
        oldRow.setId(100L);
        oldRow.setPelangganId(1L);
        oldRow.setNopol("B1234CD");
        oldRow.setTanggalMulai(LocalDate.now().minusDays(30));
        oldRow.setCurrent(true);

        when(ownershipRepository.findCurrentByNopolAndPelanggan("B1234CD", 1L)).thenReturn(Optional.of(oldRow));
        when(ownershipRepository.findCurrentByNopol("B5678EF")).thenReturn(Optional.of(new TbPelangganKendaraanEntity()));

        service.syncOnUpdate(1L, "B1234CD", "Toyota", "SUV", "B5678EF", "Honda", "Sedan", today);

        verify(ownershipRepository).closeOwnership(100L, today.minusDays(1), "nopol berubah");
        verify(ownershipRepository, never()).openOwnership(any(), any(), any(), any(), any());
        verifyNoInteractions(kendaraanService);
    }

    @Test
    @DisplayName("syncOnUpdate with master change re-points existing ownership row")
    void testSyncOnUpdate_masterChanged() {
        TbKendaraanEntity hondaSedan = new TbKendaraanEntity();
        hondaSedan.setId(20L);
        hondaSedan.setMerk("Honda");
        hondaSedan.setJenis("Sedan");

        TbPelangganKendaraanEntity row = new TbPelangganKendaraanEntity();
        row.setId(100L);
        row.setPelangganId(1L);
        row.setKendaraanId(10L);
        row.setNopol("B1234CD");
        row.setTanggalMulai(LocalDate.now().minusDays(30));
        row.setCurrent(true);

        when(ownershipRepository.findCurrentByNopolAndPelanggan("B1234CD", 1L)).thenReturn(Optional.of(row));
        when(kendaraanService.findOrCreateByMerkJenis("Toyota", "Hatchback")).thenReturn(hondaSedan);

        EntityManager em = mock(EntityManager.class);
        when(ownershipRepository.getEntityManager()).thenReturn(em);

        service.syncOnUpdate(1L, "B1234CD", "Toyota", "SUV", "B1234CD", "Toyota", "Hatchback", null);

        assertEquals(20L, row.getKendaraanId());
        verify(em).merge(row);
        verify(ownershipRepository, never()).closeOwnership(any(), any(), any());
    }

    @Test
    @DisplayName("syncOnUpdate with master change opens ownership when pelanggan has none yet")
    void testSyncOnUpdate_masterChanged_noOwnershipRow() {
        TbKendaraanEntity toyotaAgya = new TbKendaraanEntity();
        toyotaAgya.setId(30L);
        toyotaAgya.setMerk("TOYOTA");
        toyotaAgya.setJenis("Agya");

        when(ownershipRepository.findCurrentByNopolAndPelanggan("B1234CD", 1L)).thenReturn(Optional.empty());
        when(ownershipRepository.findCurrentByNopol("B1234CD")).thenReturn(Optional.empty());
        when(kendaraanService.findOrCreateByMerkJenis("Toyota", "Agya")).thenReturn(toyotaAgya);

        service.syncOnUpdate(1L, "B1234CD", "Toyota", "", "B1234CD", "Toyota", "Agya", null);

        verify(kendaraanService).findOrCreateByMerkJenis("Toyota", "Agya");
        verify(ownershipRepository).openOwnership(eq(1L), eq(30L), eq("B1234CD"), any(LocalDate.class), isNull());
    }

    @Test
    @DisplayName("syncOnUpdate with no changes does nothing")
    void testSyncOnUpdate_noop() {
        service.syncOnUpdate(1L, "B1234CD", "Toyota", "SUV", "B1234CD", "Toyota", "SUV", null);

        verifyNoInteractions(ownershipRepository);
        verifyNoInteractions(kendaraanService);
    }

    /**
     * A legacy pelanggan whose master changed but who has no ownership row yet gets one opened,
     * rather than being left unlinked.
     */
    @Test
    @DisplayName("syncOnUpdate opens a row for a legacy pelanggan whose master changed")
    void testSyncOnUpdate_masterChangedLegacyNoRow() {
        TbKendaraanEntity master = new TbKendaraanEntity();
        master.setId(20L);
        when(kendaraanService.findOrCreateByMerkJenis("Toyota", "Hatchback")).thenReturn(master);
        when(ownershipRepository.findCurrentByNopolAndPelanggan("B1234CD", 1L)).thenReturn(Optional.empty());
        when(ownershipRepository.findCurrentByNopol("B1234CD")).thenReturn(Optional.empty());

        service.syncOnUpdate(1L, "B1234CD", "Toyota", "SUV", "B1234CD", "Toyota", "Hatchback", null);

        verify(ownershipRepository).openOwnership(eq(1L), eq(20L), eq("B1234CD"), any(LocalDate.class), isNull());
    }

    /** Someone else already owns the plate, so nothing is opened for this pelanggan. */
    @Test
    @DisplayName("syncOnUpdate opens nothing when the plate is owned elsewhere")
    void testSyncOnUpdate_masterChangedPlateOwnedElsewhere() {
        TbKendaraanEntity master = new TbKendaraanEntity();
        master.setId(20L);
        when(kendaraanService.findOrCreateByMerkJenis("Toyota", "Hatchback")).thenReturn(master);
        when(ownershipRepository.findCurrentByNopolAndPelanggan("B1234CD", 1L)).thenReturn(Optional.empty());
        when(ownershipRepository.findCurrentByNopol("B1234CD"))
                .thenReturn(Optional.of(new TbPelangganKendaraanEntity()));

        service.syncOnUpdate(1L, "B1234CD", "Toyota", "SUV", "B1234CD", "Toyota", "Hatchback", null);

        verify(ownershipRepository, never()).openOwnership(any(), any(), any(), any(), any());
    }

    /**
     * Nothing changed at all: no plate change and no master change, so the ownership rows are left
     * exactly as they are.
     */
    @Test
    @DisplayName("syncOnUpdate does nothing when neither plate nor master changed")
    void testSyncOnUpdate_nothingChanged() {
        service.syncOnUpdate(1L, "B1234CD", "Toyota", "SUV", "B1234CD", "Toyota", "SUV", null);

        verifyNoInteractions(kendaraanService);
        verify(ownershipRepository, never()).openOwnership(any(), any(), any(), any(), any());
        verify(ownershipRepository, never()).closeOwnership(any(), any(), any());
    }

    /**
     * normalize() trims and treats null as empty, so "  Toyota " and "Toyota" are the same master
     * and a whitespace-only change is not a change.
     */
    @Test
    @DisplayName("syncOnUpdate ignores whitespace-only differences in merk and jenis")
    void testSyncOnUpdate_whitespaceOnlyMasterChange() {
        service.syncOnUpdate(1L, "B1234CD", "Toyota", "SUV", "B1234CD", "  Toyota ", " SUV  ", null);

        verifyNoInteractions(kendaraanService);
        verify(ownershipRepository, never()).openOwnership(any(), any(), any(), any(), any());
    }

    /** A null merk and an empty merk normalise to the same thing, so neither is a change. */
    @Test
    @DisplayName("syncOnUpdate treats a null and an empty master alike")
    void testSyncOnUpdate_nullVersusEmptyMaster() {
        service.syncOnUpdate(1L, "B1234CD", null, null, "B1234CD", "", "", null);

        verifyNoInteractions(kendaraanService);
    }

    /** A plate change away from nothing: there is no old row to close. */
    @Test
    @DisplayName("syncOnUpdate opens without closing when there was no old plate")
    void testSyncOnUpdate_noOldNopol() {
        TbKendaraanEntity master = new TbKendaraanEntity();
        master.setId(20L);
        when(kendaraanService.findOrCreateByMerkJenis("Honda", "Sedan")).thenReturn(master);
        when(ownershipRepository.findCurrentByNopol("B5678EF")).thenReturn(Optional.empty());

        service.syncOnUpdate(1L, "  ", "Honda", "Sedan", "B5678EF", "Honda", "Sedan", LocalDate.now());

        verify(ownershipRepository, never()).closeOwnership(any(), any(), any());
        verify(ownershipRepository).openOwnership(eq(1L), eq(20L), eq("B5678EF"), any(LocalDate.class), isNull());
    }

    /** A plate cleared to nothing closes the old row and opens none. */
    @Test
    @DisplayName("syncOnUpdate closes without opening when the plate is cleared")
    void testSyncOnUpdate_newNopolCleared() {
        TbPelangganKendaraanEntity row = new TbPelangganKendaraanEntity();
        row.setId(100L);
        when(ownershipRepository.findCurrentByNopolAndPelanggan("B1234CD", 1L)).thenReturn(Optional.of(row));

        service.syncOnUpdate(1L, "B1234CD", "Toyota", "SUV", "  ", "Toyota", "SUV", null);

        verify(ownershipRepository).closeOwnership(eq(100L), any(LocalDate.class), eq("nopol berubah"));
        verify(ownershipRepository, never()).openOwnership(any(), any(), any(), any(), any());
    }

    /** A plate change with no start date given opens the new row as of today. */
    @Test
    @DisplayName("syncOnUpdate defaults a nopol change to today")
    void testSyncOnUpdate_nopolChangedDefaultsToToday() {
        TbKendaraanEntity master = new TbKendaraanEntity();
        master.setId(20L);
        when(kendaraanService.findOrCreateByMerkJenis("Honda", "Sedan")).thenReturn(master);
        when(ownershipRepository.findCurrentByNopolAndPelanggan("B1234CD", 1L)).thenReturn(Optional.empty());
        when(ownershipRepository.findCurrentByNopol("B5678EF")).thenReturn(Optional.empty());

        service.syncOnUpdate(1L, "B1234CD", "Honda", "Sedan", "B5678EF", "Honda", "Sedan", null);

        verify(ownershipRepository).openOwnership(eq(1L), eq(20L), eq("B5678EF"),
                eq(LocalDate.now()), isNull());
    }

    /** A legacy master change with an explicit start date backdates the new row. */
    @Test
    @DisplayName("syncOnUpdate honours an explicit date for a legacy master change")
    void testSyncOnUpdate_masterChangedLegacyExplicitDate() {
        LocalDate backdated = LocalDate.of(2024, 1, 15);
        TbKendaraanEntity master = new TbKendaraanEntity();
        master.setId(20L);
        when(kendaraanService.findOrCreateByMerkJenis("Toyota", "Hatchback")).thenReturn(master);
        when(ownershipRepository.findCurrentByNopolAndPelanggan("B1234CD", 1L)).thenReturn(Optional.empty());
        when(ownershipRepository.findCurrentByNopol("B1234CD")).thenReturn(Optional.empty());

        service.syncOnUpdate(1L, "B1234CD", "Toyota", "SUV", "B1234CD", "Toyota", "Hatchback", backdated);

        verify(ownershipRepository).openOwnership(eq(1L), eq(20L), eq("B1234CD"), eq(backdated), isNull());
    }
}
