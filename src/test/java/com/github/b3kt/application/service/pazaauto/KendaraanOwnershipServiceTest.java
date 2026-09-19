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
}