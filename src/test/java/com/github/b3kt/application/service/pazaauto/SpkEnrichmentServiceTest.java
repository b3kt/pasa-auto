package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbKaryawanEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbKendaraanEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPelangganEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPelangganKendaraanEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSpkEntity;
import com.github.b3kt.infrastructure.persistence.entity.subentity.SpkMekanik;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbKaryawanRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbKendaraanRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbPelangganKendaraanRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbPelangganRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Parameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SpkEnrichmentService Tests")
class SpkEnrichmentServiceTest {

    @Mock
    private TbPelangganRepository pelangganRepository;

    @Mock
    private TbKaryawanRepository karyawanRepository;

    @Mock
    private TbKendaraanRepository kendaraanRepository;

    @Mock
    private TbPelangganKendaraanRepository ownershipRepository;

    @Mock
    private PanacheQuery<TbPelangganEntity> pelangganQuery;

    @Mock
    private PanacheQuery<TbKaryawanEntity> karyawanQuery;

    private SpkEnrichmentService enrichmentService;

    private TbPelangganEntity testPelangganEntity;
    private TbKaryawanEntity testKaryawanEntity;

    @BeforeEach
    void setUp() {
        enrichmentService = new SpkEnrichmentService(pelangganRepository, karyawanRepository,
                kendaraanRepository, ownershipRepository);

        testPelangganEntity = new TbPelangganEntity();
        testPelangganEntity.setId(1L);
        testPelangganEntity.setNopol("B1234CD");
        testPelangganEntity.setNamaPelanggan("John Doe");
        testPelangganEntity.setAlamat("Jl. Sudirman 123");
        testPelangganEntity.setMerk("Toyota");
        testPelangganEntity.setJenis("SUV");

        testKaryawanEntity = new TbKaryawanEntity();
        testKaryawanEntity.setId(1L);
        testKaryawanEntity.setNamaKaryawan("Mechanic One");
    }

    private TbSpkEntity createTarget(Long pelangganId, String nopol) {
        TbSpkEntity target = new TbSpkEntity();
        target.setPelangganId(pelangganId);
        target.setNopol(nopol);
        return target;
    }

    @Test
    @DisplayName("Should enrich by pelangganId when provided")
    void testEnrich_byPelangganId() {
        TbSpkEntity target = createTarget(1L, null);

        when(pelangganRepository.findById(1L)).thenReturn(testPelangganEntity);

        enrichmentService.enrich(target);

        assertEquals("John Doe", target.getNamaPelanggan());
        assertEquals("Jl. Sudirman 123", target.getAlamatPelanggan());
        assertEquals("Toyota", target.getMerkKendaraan());
        assertEquals("SUV", target.getJenisKendaraan());
        verify(pelangganRepository).findById(1L);
        verify(pelangganRepository, never()).findById(2L);
        verifyNoInteractions(ownershipRepository);
    }

    @Test
    @DisplayName("Should enrich from ownership junction when nopol is currently owned")
    void testEnrich_byNopol_ownershipWins() {
        TbSpkEntity target = createTarget(null, "B1234CD");

        TbPelangganKendaraanEntity own = new TbPelangganKendaraanEntity();
        own.setId(10L);
        own.setPelangganId(1L);
        own.setKendaraanId(77L);
        own.setNopol("B1234CD");
        when(ownershipRepository.findCurrentByNopol("B1234CD")).thenReturn(Optional.of(own));

        when(pelangganRepository.findByIdCached(1L)).thenReturn(Optional.of(testPelangganEntity));
        TbKendaraanEntity master = new TbKendaraanEntity();
        master.setId(77L);
        master.setMerk("Toyota");
        master.setJenis("SUV");
        when(kendaraanRepository.findByIdCached(77L)).thenReturn(Optional.of(master));

        enrichmentService.enrich(target);

        assertEquals(1L, target.getPelangganId());
        assertEquals("John Doe", target.getNamaPelanggan());
        assertEquals("Jl. Sudirman 123", target.getAlamatPelanggan());
        assertEquals("Toyota", target.getMerkKendaraan());
        assertEquals("SUV", target.getJenisKendaraan());
        verify(ownershipRepository).findCurrentByNopol("B1234CD");
        // Ownership resolution wins: legacy nopol lookup must not be used.
        verify(pelangganRepository, never()).find(anyString());
    }

    @Test
    @DisplayName("Should fall back to legacy pelanggan lookup when no active ownership exists")
    void testEnrich_byNopol_fallsBackToLegacy() {
        TbSpkEntity target = createTarget(null, "B1234CD");

        when(ownershipRepository.findCurrentByNopol("B1234CD")).thenReturn(Optional.empty());
        when(pelangganRepository.find(eq("nopol"), (Object) eq("B1234CD"))).thenReturn(pelangganQuery);
        when(pelangganQuery.firstResult()).thenReturn(testPelangganEntity);

        enrichmentService.enrich(target);

        assertEquals(1L, target.getPelangganId());
        assertEquals("John Doe", target.getNamaPelanggan());
        assertEquals("Toyota", target.getMerkKendaraan());
        assertEquals("SUV", target.getJenisKendaraan());
        verify(ownershipRepository).findCurrentByNopol("B1234CD");
        verify(pelangganRepository).find(eq("nopol"), (Object) eq("B1234CD"));
    }

    @Test
    @DisplayName("Should enrich by nopol when pelangganId is null")
    void testEnrich_byNopol() {
        TbSpkEntity target = createTarget(null, "B1234CD");

        when(pelangganRepository.find(eq("nopol"), (Object) eq("B1234CD"))).thenReturn(pelangganQuery);
        when(pelangganQuery.firstResult()).thenReturn(testPelangganEntity);

        enrichmentService.enrich(target);

        assertEquals(1L, target.getPelangganId());
        assertEquals("John Doe", target.getNamaPelanggan());
        assertEquals("Jl. Sudirman 123", target.getAlamatPelanggan());
        assertEquals("Toyota", target.getMerkKendaraan());
        assertEquals("SUV", target.getJenisKendaraan());
        verify(pelangganRepository).find(eq("nopol"), (Object) eq("B1234CD"));
        verify(pelangganQuery).firstResult();
    }

    @Test
    @DisplayName("Should not enrich when both pelangganId and nopol are null")
    void testEnrich_noPelanggan() {
        TbSpkEntity target = createTarget(null, null);

        enrichmentService.enrich(target);

        assertNull(target.getNamaPelanggan());
        assertNull(target.getAlamatPelanggan());
        assertNull(target.getMerkKendaraan());
        assertNull(target.getJenisKendaraan());
        verifyNoInteractions(pelangganRepository);
    }

    @Test
    @DisplayName("Should enrich with mekanik names")
    void testEnrich_withMekanikList() {
        TbSpkEntity target = createTarget(null, null);
        SpkMekanik mekanik = new SpkMekanik();
        mekanik.setId(1L);
        target.setMekanikList(List.of(mekanik));

        when(karyawanRepository.find(eq("id in :ids"), any(Parameters.class))).thenReturn(karyawanQuery);
        when(karyawanQuery.stream()).thenReturn(Stream.of(testKaryawanEntity));

        enrichmentService.enrich(target);

        assertEquals("Mechanic One", target.getNamaKaryawan());
        verify(karyawanRepository).find(eq("id in :ids"), any(Parameters.class));
    }

    @Test
    @DisplayName("Should copy km to kmSaatIni")
    void testEnrich_kmCopied() {
        TbSpkEntity target = createTarget(null, null);
        target.setKm(50000);

        enrichmentService.enrich(target);

        assertEquals(50000, target.getKmSaatIni());
    }

    @Test
    @DisplayName("Should fill required fields for list of entities")
    void testFillRequiredFields() {
        TbSpkEntity entity1 = createTarget(1L, null);
        TbSpkEntity entity2 = createTarget(2L, null);

        TbPelangganEntity pelanggan2 = new TbPelangganEntity();
        pelanggan2.setId(2L);
        pelanggan2.setNamaPelanggan("Jane Doe");
        pelanggan2.setAlamat("Jl. Thamrin 456");
        pelanggan2.setMerk("Honda");
        pelanggan2.setJenis("Sedan");

        when(pelangganRepository.findById(1L)).thenReturn(testPelangganEntity);
        when(pelangganRepository.findById(2L)).thenReturn(pelanggan2);

        enrichmentService.fillRequiredFields(List.of(entity1, entity2));

        assertEquals("John Doe", entity1.getNamaPelanggan());
        assertEquals("Jane Doe", entity2.getNamaPelanggan());
        verify(pelangganRepository).findById(1L);
        verify(pelangganRepository).findById(2L);
    }

    @Test
    @DisplayName("Should handle pelanggan not found by ID")
    void testEnrich_pelangganNotFound_byId() {
        TbSpkEntity target = createTarget(999L, null);

        when(pelangganRepository.findById(999L)).thenReturn(null);

        enrichmentService.enrich(target);

        assertNull(target.getNamaPelanggan());
        assertNull(target.getAlamatPelanggan());
        assertNull(target.getMerkKendaraan());
        assertNull(target.getJenisKendaraan());
        verify(pelangganRepository).findById(999L);
    }

    @Test
    @DisplayName("Should handle pelanggan not found by nopol")
    void testEnrich_pelangganNotFound_byNopol() {
        TbSpkEntity target = createTarget(null, "NONEXISTENT");

        when(pelangganRepository.find(eq("nopol"), (Object) eq("NONEXISTENT"))).thenReturn(pelangganQuery);
        when(pelangganQuery.firstResult()).thenReturn(null);

        enrichmentService.enrich(target);

        assertNull(target.getPelangganId());
        assertNull(target.getNamaPelanggan());
        assertNull(target.getAlamatPelanggan());
        verify(pelangganRepository).find(eq("nopol"), (Object) eq("NONEXISTENT"));
        verify(pelangganQuery).firstResult();
    }
}
