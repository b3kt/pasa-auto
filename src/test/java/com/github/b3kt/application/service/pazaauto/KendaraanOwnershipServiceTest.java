package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.application.dto.pazaauto.KendaraanOwnershipDto;
import com.github.b3kt.application.dto.pazaauto.PelangganHistoryDto;
import com.github.b3kt.application.dto.pazaauto.VehicleHistoryDto;
import com.github.b3kt.application.dto.pazaauto.VehicleTransactionDto;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbKendaraanEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbMerkKendaraanEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPelangganEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPelangganKendaraanEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPenjualanEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSpkEntity;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbKendaraanRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbMerkKendaraanRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbPelangganKendaraanRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbPelangganRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbPenjualanRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbSpkRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("KendaraanOwnershipService Tests")
class KendaraanOwnershipServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    TbPelangganKendaraanRepository ownershipRepository;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    TbKendaraanRepository kendaraanRepository;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    TbMerkKendaraanRepository merkRepository;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    TbPelangganRepository pelangganRepository;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    TbSpkRepository spkRepository;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    TbPenjualanRepository penjualanRepository;

    @InjectMocks
    KendaraanOwnershipService service;

    private TbKendaraanEntity toyotaSUV;
    private TbPelangganEntity john;
    private TbPelangganEntity jane;
    private TbPelangganKendaraanEntity activeRow;

    @BeforeEach
    void setUp() {
        toyotaSUV = new TbKendaraanEntity();
        toyotaSUV.setId(10L);
        toyotaSUV.setMerk("Toyota");
        toyotaSUV.setJenis("SUV");
        toyotaSUV.setModel("Rush");

        john = new TbPelangganEntity();
        john.setId(1L);
        john.setNopol("B1234CD");
        john.setNamaPelanggan("John Doe");
        john.setAlamat("Jl. Sudirman 123");
        john.setNoHp("0812");

        jane = new TbPelangganEntity();
        jane.setId(2L);
        jane.setNopol("B5678EF");
        jane.setNamaPelanggan("Jane Doe");
        jane.setAlamat("Jl. Thamrin 456");

        activeRow = new TbPelangganKendaraanEntity();
        activeRow.setId(100L);
        activeRow.setPelangganId(1L);
        activeRow.setKendaraanId(10L);
        activeRow.setNopol("B1234CD");
        activeRow.setTanggalMulai(LocalDate.now().minusDays(30));
        activeRow.setCurrent(true);
    }

    // ---------- transfer ----------

    @Test
    @DisplayName("transfer closes current ownership and opens one for the new customer")
    void testTransfer_success() {
        LocalDate mulai = LocalDate.now().minusDays(5);
        when(ownershipRepository.findCurrentByNopol("B1234CD")).thenReturn(Optional.of(activeRow));
        when(pelangganRepository.findByIdCached(2L)).thenReturn(Optional.of(jane));
        when(kendaraanRepository.findByIdCached(10L)).thenReturn(Optional.of(toyotaSUV));

        TbPelangganKendaraanEntity opened = new TbPelangganKendaraanEntity();
        opened.setId(200L);
        opened.setPelangganId(2L);
        opened.setKendaraanId(10L);
        opened.setNopol("B1234CD");
        opened.setTanggalMulai(mulai);
        opened.setCurrent(true);
        when(ownershipRepository.openOwnership(2L, 10L, "B1234CD", mulai, "ganti pemilik"))
                .thenReturn(opened);

        KendaraanOwnershipDto dto = service.transfer("B1234CD", 2L, mulai, "ganti pemilik");

        verify(ownershipRepository).closeOwnership(100L, mulai.minusDays(1), "ganti pemilik");
        verify(ownershipRepository).openOwnership(2L, 10L, "B1234CD", mulai, "ganti pemilik");
        assertEquals(2L, dto.getIdPelanggan());
        assertEquals("Jane Doe", dto.getNamaPelanggan());
        assertEquals("B1234CD", dto.getNopol());
        assertEquals("Toyota", dto.getMerk());
    }

    @Test
    @DisplayName("transfer defaults tanggalAwal to today")
    void testTransfer_defaultsDateToToday() {
        when(ownershipRepository.findCurrentByNopol("B1234CD")).thenReturn(Optional.of(activeRow));
        when(pelangganRepository.findByIdCached(2L)).thenReturn(Optional.of(jane));
        when(kendaraanRepository.findByIdCached(10L)).thenReturn(Optional.of(toyotaSUV));

        TbPelangganKendaraanEntity opened = new TbPelangganKendaraanEntity();
        opened.setId(200L);
        opened.setPelangganId(2L);
        opened.setKendaraanId(10L);
        opened.setNopol("B1234CD");
        opened.setTanggalMulai(LocalDate.now());
        opened.setCurrent(true);
        when(ownershipRepository.openOwnership(eq(2L), eq(10L), eq("B1234CD"), any(LocalDate.class), isNull()))
                .thenReturn(opened);

        service.transfer("B1234CD", 2L, null, null);

        verify(ownershipRepository).openOwnership(eq(2L), eq(10L), eq("B1234CD"), any(LocalDate.class), isNull());
    }

    @Test
    @DisplayName("transfer throws when nopol has no active ownership")
    void testTransfer_noActiveOwnership() {
        when(ownershipRepository.findCurrentByNopol("UNKNOWN")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.transfer("UNKNOWN", 2L, null, null));
        assertTrue(ex.getMessage().contains("No active ownership"));
    }

    @Test
    @DisplayName("transfer throws when target pelanggan does not exist")
    void testTransfer_pelangganNotFound() {
        when(ownershipRepository.findCurrentByNopol("B1234CD")).thenReturn(Optional.of(activeRow));
        when(pelangganRepository.findByIdCached(999L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.transfer("B1234CD", 999L, null, null));
        assertTrue(ex.getMessage().contains("Pelanggan not found"));
    }

    @Test
    @DisplayName("transfer throws when vehicle already owned by target pelanggan")
    void testTransfer_sameOwner() {
        when(ownershipRepository.findCurrentByNopol("B1234CD")).thenReturn(Optional.of(activeRow));
        when(pelangganRepository.findByIdCached(1L)).thenReturn(Optional.of(john));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.transfer("B1234CD", 1L, null, null));
        assertTrue(ex.getMessage().contains("already owned"));
    }

    @Test
    @DisplayName("transfer throws when date precedes current ownership start")
    void testTransfer_dateTooEarly() {
        when(ownershipRepository.findCurrentByNopol("B1234CD")).thenReturn(Optional.of(activeRow));
        when(pelangganRepository.findByIdCached(2L)).thenReturn(Optional.of(jane));

        LocalDate tooEarly = activeRow.getTanggalMulai().minusDays(10);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.transfer("B1234CD", 2L, tooEarly, null));
        assertTrue(ex.getMessage().contains("cannot precede"));
    }

    // ---------- attach ----------

    @Test
    @DisplayName("attach reuses existing vehicle master")
    void testAttach_reusesMaster() {
        when(pelangganRepository.findByIdCached(1L)).thenReturn(Optional.of(john));
        when(ownershipRepository.findCurrentByNopol("B9999XX")).thenReturn(Optional.empty());
        when(kendaraanRepository.findByMerkAndJenis("Toyota", "SUV")).thenReturn(Optional.of(toyotaSUV));

        TbPelangganKendaraanEntity opened = new TbPelangganKendaraanEntity();
        opened.setId(300L);
        opened.setPelangganId(1L);
        opened.setKendaraanId(10L);
        opened.setNopol("B9999XX");
        opened.setTanggalMulai(LocalDate.now());
        opened.setCurrent(true);
        when(ownershipRepository.openOwnership(eq(1L), eq(10L), eq("B9999XX"), any(LocalDate.class), any()))
                .thenReturn(opened);

        KendaraanOwnershipDto dto = service.attach(1L, "B9999XX", "Toyota", "SUV", null, "kendaraan baru");

        verify(kendaraanRepository, never()).getEntityManager();
        verify(ownershipRepository).openOwnership(eq(1L), eq(10L), eq("B9999XX"), any(LocalDate.class), eq("kendaraan baru"));
        assertEquals(10L, dto.getIdKendaraan());
        assertEquals("B9999XX", dto.getNopol());
    }

    @Test
    @DisplayName("attach creates a new master when (merk, jenis) is unknown")
    void testAttach_createsMaster() {
        when(pelangganRepository.findByIdCached(1L)).thenReturn(Optional.of(john));
        when(ownershipRepository.findCurrentByNopol("B9999XX")).thenReturn(Optional.empty());
        when(kendaraanRepository.findByMerkAndJenis("Suzuki", "Hatchback")).thenReturn(Optional.empty());
        TbMerkKendaraanEntity suzuki = new TbMerkKendaraanEntity();
        suzuki.setId(8L);
        suzuki.setNama("SUZUKI");
        when(merkRepository.findOrCreateByNama("Suzuki")).thenReturn(suzuki);

        EntityManager em = mock(EntityManager.class);
        TbKendaraanEntity newMaster = new TbKendaraanEntity();
        newMaster.setId(77L);
        newMaster.setMerk("Suzuki");
        newMaster.setJenis("Hatchback");
        when(kendaraanRepository.getEntityManager()).thenReturn(em);
        when(em.merge(any(TbKendaraanEntity.class))).thenAnswer(inv -> {
            TbKendaraanEntity e = inv.getArgument(0);
            e.setId(77L);
            return e;
        });

        TbPelangganKendaraanEntity opened = new TbPelangganKendaraanEntity();
        opened.setId(301L);
        opened.setPelangganId(1L);
        opened.setKendaraanId(77L);
        opened.setNopol("B9999XX");
        opened.setTanggalMulai(LocalDate.now());
        opened.setCurrent(true);
        when(ownershipRepository.openOwnership(eq(1L), eq(77L), eq("B9999XX"), any(LocalDate.class), isNull()))
                .thenReturn(opened);
        when(kendaraanRepository.findByIdCached(77L)).thenReturn(Optional.of(newMaster));

        KendaraanOwnershipDto dto = service.attach(1L, "B9999XX", "Suzuki", "Hatchback", null, null);

        verify(em).merge(argThat((TbKendaraanEntity e) -> Long.valueOf(8L).equals(e.getMerkId())));
        assertEquals(77L, dto.getIdKendaraan());
        assertEquals("Suzuki", dto.getMerk());
    }

    @Test
    @DisplayName("attach throws when nopol already owned elsewhere")
    void testAttach_conflictingNopol() {
        when(pelangganRepository.findByIdCached(1L)).thenReturn(Optional.of(john));
        when(ownershipRepository.findCurrentByNopol("B1234CD")).thenReturn(Optional.of(activeRow));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.attach(1L, "B1234CD", "Toyota", "SUV", null, null));
        assertTrue(ex.getMessage().contains("already owned"));
    }

    @Test
    @DisplayName("attach throws when pelanggan does not exist")
    void testAttach_pelangganNotFound() {
        when(pelangganRepository.findByIdCached(999L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.attach(999L, "B9999XX", "Toyota", "SUV", null, null));
        assertTrue(ex.getMessage().contains("Pelanggan not found"));
    }

    @Test
    @DisplayName("attach throws when nopol is blank")
    void testAttach_blankNopol() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.attach(1L, "  ", "Toyota", "SUV", null, null));
        assertTrue(ex.getMessage().contains("nopol is required"));
        verifyNoInteractions(pelangganRepository);
    }

    // ---------- lookups ----------

    @Test
    @DisplayName("getVehicleByNopol maps current ownership with master and owner")
    void testGetVehicleByNopol_found() {
        when(ownershipRepository.findCurrentByNopol("B1234CD")).thenReturn(Optional.of(activeRow));
        when(kendaraanRepository.findByIdCached(10L)).thenReturn(Optional.of(toyotaSUV));
        when(pelangganRepository.findByIdCached(1L)).thenReturn(Optional.of(john));

        KendaraanOwnershipDto dto = service.getVehicleByNopol("B1234CD");

        assertNotNull(dto);
        assertEquals("B1234CD", dto.getNopol());
        assertEquals("John Doe", dto.getNamaPelanggan());
        assertEquals("Toyota", dto.getMerk());
        assertEquals("Jl. Sudirman 123", dto.getAlamatPelanggan());
        assertEquals("0812", dto.getNoHpPelanggan());
        assertTrue(dto.isCurrent());
    }

    @Test
    @DisplayName("getVehicleByNopol returns null when not found")
    void testGetVehicleByNopol_notFound() {
        when(ownershipRepository.findCurrentByNopol("UNKNOWN")).thenReturn(Optional.empty());

        assertNull(service.getVehicleByNopol("UNKNOWN"));
    }

    @Test
    @DisplayName("getVehicleHistory returns null for unknown nopol")
    void testGetVehicleHistory_notFound() {
        when(ownershipRepository.findByNopolOrderByTanggalMulai("UNKNOWN")).thenReturn(List.of());

        assertNull(service.getVehicleHistory("UNKNOWN"));
    }

    @Test
    @DisplayName("getVehicleHistory contains owners and transactions")
    void testGetVehicleHistory_found() {
        when(ownershipRepository.findByNopolOrderByTanggalMulai("B1234CD")).thenReturn(List.of(activeRow));
        when(kendaraanRepository.findByIdCached(10L)).thenReturn(Optional.of(toyotaSUV));
        when(pelangganRepository.findByIdCached(1L)).thenReturn(Optional.of(john));

        VehicleHistoryDto dto = service.getVehicleHistory("B1234CD");

        assertNotNull(dto);
        assertEquals("B1234CD", dto.getNopol());
        assertEquals("Toyota", dto.getMerk());
        assertEquals(1, dto.getOwners().size());
        assertEquals("John Doe", dto.getOwners().get(0).getNamaPelanggan());
        assertNotNull(dto.getTransactions());
    }

    @Test
    @DisplayName("listOwnerHistoryByMasterId maps each ownership row")
    void testListOwnerHistoryByMasterId() {
        when(ownershipRepository.findByKendaraanIdOrderByTanggalMulai(10L)).thenReturn(List.of(activeRow));
        when(kendaraanRepository.findByIdCached(10L)).thenReturn(Optional.of(toyotaSUV));
        when(pelangganRepository.findByIdCached(1L)).thenReturn(Optional.of(john));

        List<KendaraanOwnershipDto> result = service.listOwnerHistoryByMasterId(10L);

        assertEquals(1, result.size());
        assertEquals("John Doe", result.get(0).getNamaPelanggan());
    }

    @Test
    @DisplayName("listVehiclesByPelanggan returns current ownership rows")
    void testListVehiclesByPelanggan() {
        when(ownershipRepository.findByPelangganId(1L)).thenReturn(List.of(activeRow));
        when(kendaraanRepository.findByIdCached(10L)).thenReturn(Optional.of(toyotaSUV));
        when(pelangganRepository.findByIdCached(1L)).thenReturn(Optional.of(john));

        List<KendaraanOwnershipDto> result = service.listVehiclesByPelanggan(1L);

        assertEquals(1, result.size());
        assertEquals("B1234CD", result.get(0).getNopol());
    }

    @Test
    @DisplayName("listCurrentVehiclesByPelanggan only returns current rows")
    void testListCurrentVehiclesByPelanggan() {
        when(ownershipRepository.findByPelangganIdCurrent(1L)).thenReturn(List.of(activeRow));
        when(kendaraanRepository.findByIdCached(10L)).thenReturn(Optional.of(toyotaSUV));
        when(pelangganRepository.findByIdCached(1L)).thenReturn(Optional.of(john));

        List<KendaraanOwnershipDto> result = service.listCurrentVehiclesByPelanggan(1L);

        assertEquals(1, result.size());
        assertTrue(result.get(0).isCurrent());
    }

    @Test
    @DisplayName("getPelangganHistory returns null for unknown pelanggan")
    void testGetPelangganHistory_notFound() {
        when(pelangganRepository.findByIdCached(999L)).thenReturn(Optional.empty());

        assertNull(service.getPelangganHistory(999L));
    }

    @Test
    @DisplayName("getPelangganHistory aggregates vehicles and transactions")
    void testGetPelangganHistory_found() {
        when(pelangganRepository.findByIdCached(1L)).thenReturn(Optional.of(john));
        when(ownershipRepository.findByPelangganId(1L)).thenReturn(List.of(activeRow));
        when(kendaraanRepository.findByIdCached(10L)).thenReturn(Optional.of(toyotaSUV));

        PelangganHistoryDto dto = service.getPelangganHistory(1L);

        assertNotNull(dto);
        assertEquals(1L, dto.getIdPelanggan());
        assertEquals("John Doe", dto.getNamaPelanggan());
        assertEquals(1, dto.getVehicles().size());
        assertNotNull(dto.getTransactions());
    }

    // ---------- transactions ----------

    @Test
    @DisplayName("findTransactionsByNopol builds SPK transactions with linked penjualan")
    void testFindTransactionsByNopol() {
        TbSpkEntity spk = new TbSpkEntity();
        spk.setId(1L);
        spk.setNoSpk("SPK-001");
        spk.setStatusSpk("PROSES");
        spk.setNopol("B1234CD");
        spk.setPelangganId(1L);
        spk.setTanggalJamSpk(LocalDateTime.now().minusDays(1).toString());
        spk.setCreatedAt(LocalDateTime.now().minusDays(1));

        TbPenjualanEntity penjualan = new TbPenjualanEntity();
        penjualan.setNoPenjualan("PJ-001");
        penjualan.setNoSpk("SPK-001");
        penjualan.setGrandTotal(new BigDecimal("1500000"));
        when(penjualanRepository.find("noSpk", "SPK-001").firstResult()).thenReturn(penjualan);
        when(spkRepository.find("nopol", "B1234CD").list()).thenReturn(List.of(spk));
        when(pelangganRepository.findByIdCached(1L)).thenReturn(Optional.of(john));

        List<VehicleTransactionDto> result = service.findTransactionsByNopol("B1234CD");

        assertEquals(1, result.size());
        VehicleTransactionDto dto = result.get(0);
        assertEquals("SPK", dto.getJenis());
        assertEquals("SPK-001", dto.getNoSpk());
        assertEquals("PJ-001", dto.getNoPenjualan());
        assertEquals("John Doe", dto.getOwnerNamaPelanggan());
        assertEquals(1L, dto.getOwnerPelangganId());
        assertEquals(new BigDecimal("1500000"), dto.getGrandTotal());
    }

    @Test
    @DisplayName("findTransactionsByPelanggan combines SPK and penjualan rows")
    void testFindTransactionsByPelanggan() {
        TbSpkEntity spk = new TbSpkEntity();
        spk.setId(1L);
        spk.setNoSpk("SPK-001");
        spk.setStatusSpk("PROSES");
        spk.setPelangganId(1L);
        spk.setCreatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));

        TbPenjualanEntity penjualan = new TbPenjualanEntity();
        penjualan.setId(5L);
        penjualan.setNoPenjualan("PJ-001");
        penjualan.setNoSpk("SPK-001");
        penjualan.setStatusPembayaran("LUNAS");
        penjualan.setPelangganId(1L);
        penjualan.setGrandTotal(new BigDecimal("750000"));
        penjualan.setCreatedAt(LocalDateTime.of(2026, 1, 2, 9, 30));

        when(spkRepository.find("pelangganId", 1L).list()).thenReturn(List.of(spk));
        when(penjualanRepository.find("pelangganId", 1L).list()).thenReturn(List.of(penjualan));
        when(pelangganRepository.findByIdCached(1L)).thenReturn(Optional.of(john));

        List<VehicleTransactionDto> result = service.findTransactionsByPelanggan(1L);

        assertEquals(2, result.size());
        VehicleTransactionDto spkDto = result.get(0);
        assertEquals("SPK", spkDto.getJenis());
        assertEquals("John Doe", spkDto.getOwnerNamaPelanggan());

        VehicleTransactionDto penjualanDto = result.get(1);
        assertEquals("PENJUALAN", penjualanDto.getJenis());
        assertEquals("LUNAS", penjualanDto.getStatus());
        assertEquals("John Doe", penjualanDto.getOwnerNamaPelanggan());
    }

    @Test
    @DisplayName("findTransactionsByPelanggan resolves owner from SPK nopol when penjualan has no owner")
    void testFindTransactionsByPelanggan_ownerFromSpkNopol() {
        TbSpkEntity spk = new TbSpkEntity();
        spk.setId(1L);
        spk.setNoSpk("SPK-001");
        spk.setNopol("B1234CD");
        spk.setCreatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));

        TbPenjualanEntity penjualan = new TbPenjualanEntity();
        penjualan.setId(5L);
        penjualan.setNoPenjualan("PJ-001");
        penjualan.setNoSpk("SPK-001");
        penjualan.setCreatedAt(LocalDateTime.of(2026, 1, 2, 9, 30));

        TbPelangganKendaraanEntity ownerOnDate = new TbPelangganKendaraanEntity();
        ownerOnDate.setId(200L);
        ownerOnDate.setPelangganId(2L);
        ownerOnDate.setKendaraanId(10L);
        ownerOnDate.setNopol("B1234CD");
        ownerOnDate.setTanggalMulai(LocalDate.of(2026, 1, 1));
        ownerOnDate.setCurrent(true);

        when(spkRepository.find("pelangganId", 1L).list()).thenReturn(List.of());
        when(penjualanRepository.find("pelangganId", 1L).list()).thenReturn(List.of(penjualan));
        when(spkRepository.find("noSpk", "SPK-001").firstResult()).thenReturn(spk);
        when(ownershipRepository.findByNopolActiveOn(eq("B1234CD"), any(LocalDate.class)))
                .thenReturn(Optional.of(ownerOnDate));
        when(pelangganRepository.findByIdCached(2L)).thenReturn(Optional.of(jane));

        List<VehicleTransactionDto> result = service.findTransactionsByPelanggan(1L);

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).getOwnerPelangganId());
        assertEquals("Jane Doe", result.get(0).getOwnerNamaPelanggan());
    }

    // --- findTransactionsByPelanggan -------------------------------------------------------

    private TbSpkEntity spk(String noSpk, String tanggalJamSpk) {
        TbSpkEntity spk = new TbSpkEntity();
        spk.setNoSpk(noSpk);
        spk.setStatusSpk("SELESAI");
        spk.setTanggalJamSpk(tanggalJamSpk);
        return spk;
    }

    private TbPenjualanEntity penjualan(String noPenjualan, String noSpk) {
        TbPenjualanEntity p = new TbPenjualanEntity();
        p.setNoPenjualan(noPenjualan);
        p.setNoSpk(noSpk);
        p.setStatusPembayaran("LUNAS");
        p.setGrandTotal(new BigDecimal("250000"));
        return p;
    }

    /** No SPK and no penjualan for the customer is an empty history, not a failure. */
    @Test
    @DisplayName("findTransactionsByPelanggan returns nothing when there is no activity")
    void testFindTransactionsByPelanggan_empty() {
        when(spkRepository.find("pelangganId", 1L).list()).thenReturn(List.of());
        when(penjualanRepository.find("pelangganId", 1L).list()).thenReturn(List.of());

        assertTrue(service.findTransactionsByPelanggan(1L).isEmpty());
    }

    @Test
    @DisplayName("findTransactionsByPelanggan includes both SPK and penjualan rows")
    void testFindTransactionsByPelanggan_bothKinds() {
        TbSpkEntity s = spk("SPK-1", "2024-03-01 09:30:00");
        s.setPelangganId(1L);
        when(spkRepository.find("pelangganId", 1L).list()).thenReturn(List.of(s));
        when(penjualanRepository.find("noSpk", "SPK-1").firstResult()).thenReturn(null);

        TbPenjualanEntity p = penjualan("PJ-1", null);
        p.setPelangganId(1L);
        when(penjualanRepository.find("pelangganId", 1L).list()).thenReturn(List.of(p));
        when(pelangganRepository.findByIdCached(1L)).thenReturn(Optional.of(john));

        List<VehicleTransactionDto> result = service.findTransactionsByPelanggan(1L);

        assertEquals(2, result.size());
        assertEquals(List.of("SPK", "PENJUALAN"), result.stream().map(VehicleTransactionDto::getJenis).toList());
        assertEquals("PJ-1", result.get(1).getNoPenjualan());
        assertEquals(1L, result.get(1).getOwnerPelangganId());
        assertEquals(john.getNamaPelanggan(), result.get(1).getOwnerNamaPelanggan());
    }

    /** A penjualan dated by its own timestamp uses that, in the system zone. */
    @Test
    @DisplayName("findTransactionsByPelanggan dates a penjualan from its timestamp")
    void testFindTransactionsByPelanggan_dateFromTimestamp() {
        when(spkRepository.find("pelangganId", 1L).list()).thenReturn(List.of());
        TbPenjualanEntity p = penjualan("PJ-1", null);
        p.setPelangganId(1L);
        p.setTanggalJamPenjualan(new java.util.Date(1709289000000L));
        when(penjualanRepository.find("pelangganId", 1L).list()).thenReturn(List.of(p));
        when(pelangganRepository.findByIdCached(1L)).thenReturn(Optional.empty());

        VehicleTransactionDto dto = service.findTransactionsByPelanggan(1L).getFirst();

        assertNotNull(dto.getTanggal());
        assertEquals(dto.getTanggal().toLocalDate(), dto.getTanggalOnly());
    }

    /** Without its own timestamp a penjualan falls back to createdAt. */
    @Test
    @DisplayName("findTransactionsByPelanggan falls back to createdAt")
    void testFindTransactionsByPelanggan_dateFromCreatedAt() {
        when(spkRepository.find("pelangganId", 1L).list()).thenReturn(List.of());
        TbPenjualanEntity p = penjualan("PJ-1", null);
        p.setPelangganId(1L);
        p.setCreatedAt(LocalDateTime.of(2024, 3, 1, 9, 30));
        when(penjualanRepository.find("pelangganId", 1L).list()).thenReturn(List.of(p));
        when(pelangganRepository.findByIdCached(1L)).thenReturn(Optional.empty());

        VehicleTransactionDto dto = service.findTransactionsByPelanggan(1L).getFirst();

        assertEquals(LocalDateTime.of(2024, 3, 1, 9, 30), dto.getTanggal());
        assertEquals(LocalDate.of(2024, 3, 1), dto.getTanggalOnly());
    }

    // --- resolveOwnerFromSpk ---------------------------------------------------------------

    /** A penjualan with no customer of its own borrows the one on its SPK. */
    @Test
    @DisplayName("a penjualan without a customer takes the owner from its SPK")
    void testResolveOwnerFromSpk_viaSpkPelangganId() {
        when(spkRepository.find("pelangganId", 1L).list()).thenReturn(List.of());
        TbPenjualanEntity p = penjualan("PJ-1", "SPK-9");
        when(penjualanRepository.find("pelangganId", 1L).list()).thenReturn(List.of(p));

        TbSpkEntity source = spk("SPK-9", null);
        source.setPelangganId(2L);
        when(spkRepository.find("noSpk", "SPK-9").firstResult()).thenReturn(source);
        when(pelangganRepository.findByIdCached(2L)).thenReturn(Optional.of(jane));

        VehicleTransactionDto dto = service.findTransactionsByPelanggan(1L).getFirst();

        assertEquals(2L, dto.getOwnerPelangganId());
        assertEquals(jane.getNamaPelanggan(), dto.getOwnerNamaPelanggan());
    }

    /** An SPK that no longer exists leaves the owner unresolved rather than failing. */
    @Test
    @DisplayName("a missing SPK leaves the owner unresolved")
    void testResolveOwnerFromSpk_spkMissing() {
        when(spkRepository.find("pelangganId", 1L).list()).thenReturn(List.of());
        when(penjualanRepository.find("pelangganId", 1L).list()).thenReturn(List.of(penjualan("PJ-1", "SPK-GONE")));
        when(spkRepository.find("noSpk", "SPK-GONE").firstResult()).thenReturn(null);

        VehicleTransactionDto dto = service.findTransactionsByPelanggan(1L).getFirst();

        assertNull(dto.getOwnerPelangganId());
        assertNull(dto.getOwnerNamaPelanggan());
    }

    /** With no customer id anywhere, the SPK's plate and date locate the owner of record. */
    @Test
    @DisplayName("the owner is resolved from the plate and date when no id is recorded")
    void testResolveOwnerFromSpk_viaOwnershipLookup() {
        when(spkRepository.find("pelangganId", 1L).list()).thenReturn(List.of());
        TbPenjualanEntity p = penjualan("PJ-1", "SPK-9");
        p.setCreatedAt(LocalDateTime.of(2024, 3, 1, 9, 30));
        when(penjualanRepository.find("pelangganId", 1L).list()).thenReturn(List.of(p));

        TbSpkEntity source = spk("SPK-9", null);
        source.setNopol("B 1234 CD");
        when(spkRepository.find("noSpk", "SPK-9").firstResult()).thenReturn(source);
        when(ownershipRepository.findByNopolActiveOn(eq("B 1234 CD"), any(LocalDate.class)))
                .thenReturn(Optional.of(activeRow));
        when(pelangganRepository.findByIdCached(activeRow.getPelangganId())).thenReturn(Optional.of(john));

        VehicleTransactionDto dto = service.findTransactionsByPelanggan(1L).getFirst();

        assertEquals(activeRow.getPelangganId(), dto.getOwnerPelangganId());
        assertEquals(john.getNamaPelanggan(), dto.getOwnerNamaPelanggan());
    }

    /** When nothing resolves to an id, the name written on the SPK is better than nothing. */
    @Test
    @DisplayName("the SPK's customer name is used when no id can be resolved")
    void testResolveOwnerFromSpk_fallsBackToName() {
        when(spkRepository.find("pelangganId", 1L).list()).thenReturn(List.of());
        when(penjualanRepository.find("pelangganId", 1L).list()).thenReturn(List.of(penjualan("PJ-1", "SPK-9")));

        TbSpkEntity source = spk("SPK-9", null);
        source.setNamaPelanggan("Walk-in Budi");
        when(spkRepository.find("noSpk", "SPK-9").firstResult()).thenReturn(source);

        VehicleTransactionDto dto = service.findTransactionsByPelanggan(1L).getFirst();

        assertNull(dto.getOwnerPelangganId());
        assertEquals("Walk-in Budi", dto.getOwnerNamaPelanggan());
    }

    // --- transactionsFor / parseTanggal ----------------------------------------------------

    /** The SPK date is free text in the database, so several shapes have to be understood. */
    @org.junit.jupiter.params.ParameterizedTest(name = "\"{0}\" parses to {1}")
    @org.junit.jupiter.params.provider.CsvSource({
            "2024-03-01 09:30:00, 2024-03-01T09:30",
            "2024-03-01 09:30,    2024-03-01T09:30",
            "01/03/2024 09:30,    2024-03-01T09:30",
            "2024-03-01,          2024-03-01T00:00"
    })
    @DisplayName("an SPK date is parsed from any of its stored formats")
    void testParseTanggal_formats(String stored, String expected) {
        when(spkRepository.find("nopol", "B 1234 CD").list()).thenReturn(List.of(spk("SPK-1", stored)));
        when(penjualanRepository.find("noSpk", "SPK-1").firstResult()).thenReturn(null);

        VehicleTransactionDto dto = service.findTransactionsByNopol("B 1234 CD").getFirst();

        assertEquals(LocalDateTime.parse(expected), dto.getTanggal());
        assertEquals(LocalDateTime.parse(expected).toLocalDate(), dto.getTanggalOnly());
    }

    /** An unparseable or absent date falls back to createdAt, and otherwise stays null. */
    @Test
    @DisplayName("an unreadable SPK date falls back to createdAt, then to nothing")
    void testParseTanggal_fallbacks() {
        TbSpkEntity withCreatedAt = spk("SPK-1", "not a date");
        withCreatedAt.setCreatedAt(LocalDateTime.of(2024, 5, 4, 8, 0));
        TbSpkEntity withNothing = spk("SPK-2", "   ");
        when(spkRepository.find("nopol", "B 1234 CD").list()).thenReturn(List.of(withCreatedAt, withNothing));
        when(penjualanRepository.find("noSpk", "SPK-1").firstResult()).thenReturn(null);
        when(penjualanRepository.find("noSpk", "SPK-2").firstResult()).thenReturn(null);

        List<VehicleTransactionDto> result = service.findTransactionsByNopol("B 1234 CD");

        assertEquals(LocalDateTime.of(2024, 5, 4, 8, 0), result.get(0).getTanggal());
        assertNull(result.get(1).getTanggal());
        assertNull(result.get(1).getTanggalOnly());
    }

    /** A matching penjualan is linked onto the SPK row so totals show in the history. */
    @Test
    @DisplayName("a matching penjualan is linked onto the SPK row")
    void testTransactionsFor_linksPenjualan() {
        TbSpkEntity s = spk("SPK-1", "2024-03-01 09:30:00");
        s.setNamaPelanggan("Walk-in Budi");
        when(spkRepository.find("nopol", "B 1234 CD").list()).thenReturn(List.of(s));
        when(penjualanRepository.find("noSpk", "SPK-1").firstResult()).thenReturn(penjualan("PJ-1", "SPK-1"));

        VehicleTransactionDto dto = service.findTransactionsByNopol("B 1234 CD").getFirst();

        assertEquals("PJ-1", dto.getNoPenjualan());
        assertEquals(new BigDecimal("250000"), dto.getGrandTotal());
        assertEquals("Walk-in Budi", dto.getOwnerNamaPelanggan());
    }

    /** A plate is what ownership is keyed on, so attaching without one is refused up front. */
    @Test
    @DisplayName("attach refuses a missing or blank nopol")
    void testAttach_requiresNopol() {
        for (String nopol : new String[]{null, "", "   "}) {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.attach(1L, nopol, "Toyota", "SUV", null, null));
            assertEquals("nopol is required", ex.getMessage());
        }
        verifyNoInteractions(ownershipRepository);
    }

    /** A master created on the fly normalises a null jenis to empty rather than a null column. */
    @Test
    @DisplayName("attach creates a master with an empty jenis when none is given")
    void testAttach_createsMasterWithNullJenis() {
        when(pelangganRepository.findByIdCached(1L)).thenReturn(Optional.of(john));
        when(ownershipRepository.findCurrentByNopol("B9999XX")).thenReturn(Optional.empty());
        when(kendaraanRepository.findByMerkAndJenis("Suzuki", null)).thenReturn(Optional.empty());

        TbMerkKendaraanEntity merk = new TbMerkKendaraanEntity();
        merk.setId(3L);
        merk.setNama("SUZUKI");
        when(merkRepository.findOrCreateByNama("Suzuki")).thenReturn(merk);

        EntityManager em = mock(EntityManager.class);
        when(kendaraanRepository.getEntityManager()).thenReturn(em);
        when(em.merge(any(TbKendaraanEntity.class))).thenAnswer(inv -> {
            TbKendaraanEntity created = inv.getArgument(0);
            created.setId(77L);
            return created;
        });

        service.attach(1L, "B9999XX", "Suzuki", null, null, null);

        ArgumentCaptor<TbKendaraanEntity> captor = ArgumentCaptor.forClass(TbKendaraanEntity.class);
        verify(em).merge(captor.capture());
        assertEquals("", captor.getValue().getJenis());
        assertEquals("SUZUKI", captor.getValue().getMerk());
    }

    /** An explicit start date is honoured; only an absent one falls back to today. */
    @Test
    @DisplayName("attach honours an explicit tanggalMulai")
    void testAttach_explicitTanggalMulai() {
        LocalDate backdated = LocalDate.of(2024, 1, 15);
        when(pelangganRepository.findByIdCached(1L)).thenReturn(Optional.of(john));
        when(ownershipRepository.findCurrentByNopol("B9999XX")).thenReturn(Optional.empty());
        when(kendaraanRepository.findByMerkAndJenis("Toyota", "SUV")).thenReturn(Optional.of(toyotaSUV));
        when(ownershipRepository.openOwnership(any(), any(), any(), any(), any()))
                .thenReturn(activeRow);

        service.attach(1L, "B9999XX", "Toyota", "SUV", backdated, "kendaraan baru");

        verify(ownershipRepository).openOwnership(eq(1L), eq(toyotaSUV.getId()), eq("B9999XX"),
                eq(backdated), eq("kendaraan baru"));
    }
}
