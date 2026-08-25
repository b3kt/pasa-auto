package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.application.dto.pazaauto.PenjualanPrintDto;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.*;
import com.github.b3kt.infrastructure.persistence.entity.subentity.SpkMekanik;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.*;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Parameters;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TbPenjualanService Tests")
class TbPenjualanServiceTest {

    @Mock TbPenjualanRepository repository;
    @Mock TbPenjualanDetailRepository penjualanDetailRepository;
    @Mock TbSpkRepository spkRepository;
    @Mock TbSpkService spkService;
    @Mock TbSpkDetailService spkDetailService;
    @Mock TbPelangganService pelangganService;
    @Mock TbKendaraanRepository kendaraanRepository;
    @Mock TbKaryawanRepository karyawanRepository;
    @Mock TbJasaRepository jasaRepository;
    @Mock TbSparepartRepository sparepartRepository;
    @Mock TbBarangRepository barangRepository;
    @Mock PanacheQuery<TbPenjualanEntity> panacheQuery;

    @InjectMocks TbPenjualanService penjualanService;

    private TbPenjualanEntity testEntity;

    @BeforeEach
    void setUp() {
        testEntity = new TbPenjualanEntity();
        testEntity.setId(1L);
        testEntity.setNoPenjualan("PJ001");
        testEntity.setNoSpk("SPK001");
        testEntity.setPelangganId(1L);
        testEntity.setGrandTotal(BigDecimal.valueOf(500000));
        testEntity.setTanggalJamPenjualan(new Date());
        testEntity.setStatusPembayaran("LUNAS");
        testEntity.setMetodePembayaran("CASH");
        testEntity.setDiscount(BigDecimal.valueOf(10000));
        testEntity.setUangDibayar(BigDecimal.valueOf(510000));
        testEntity.setKembalian(BigDecimal.valueOf(10000));
        testEntity.setKendaraanId(1L);
    }

    // ─── findByNoPenjualan ────────────────────────────────────────────

    @Nested
    @DisplayName("findByNoPenjualan")
    class FindByNoPenjualan {

        @Test
        @DisplayName("returns null when penjualan not found")
        void returnsNull() {
            when(repository.findByNoPenjualan("NONE")).thenReturn(null);

            assertNull(penjualanService.findByNoPenjualan("NONE"));
        }

        @Test
        @DisplayName("returns entity when found with pelangganId set — no SPK lookup")
        void foundWithPelangganId() {
            when(repository.findByNoPenjualan("PJ001")).thenReturn(testEntity);

            TbPenjualanEntity result = penjualanService.findByNoPenjualan("PJ001");

            assertNotNull(result);
            assertEquals("PJ001", result.getNoPenjualan());
            assertEquals(1L, result.getPelangganId());
            verifyNoInteractions(spkService, pelangganService, spkDetailService);
        }

        @Test
        @DisplayName("fills transient fields via SPK chain when pelangganId is null")
        void fillsViaSpkChain() {
            TbPenjualanEntity entity = new TbPenjualanEntity();
            entity.setNoPenjualan("PJ002");
            entity.setNoSpk("SPK002");
            entity.setPelangganId(null);

            TbSpkEntity spkEntity = new TbSpkEntity();
            spkEntity.setPelangganId(2L);

            TbPelangganEntity pelanggan = new TbPelangganEntity();
            pelanggan.setNamaPelanggan("Budi");
            pelanggan.setAlamat("Bandung");
            pelanggan.setMerk("Honda");
            pelanggan.setJenis("Beat");
            pelanggan.setNopol("D1234AB");

            TbSpkDetailEntity spkDetail = new TbSpkDetailEntity();

            when(repository.findByNoPenjualan("PJ002")).thenReturn(entity);
            when(spkService.findByNoSpk("SPK002")).thenReturn(spkEntity);
            when(spkDetailService.findByNoSpk("SPK002")).thenReturn(List.of(spkDetail));
            when(pelangganService.findById(2L)).thenReturn(pelanggan);

            TbPenjualanEntity result = penjualanService.findByNoPenjualan("PJ002");

            assertNotNull(result);
            assertEquals("Budi", result.getNamaPelanggan());
            assertEquals("Bandung", result.getAlamatPelanggan());
            assertEquals("Honda", result.getMerkKendaraan());
            assertEquals("Beat", result.getJenisKendaraan());
            assertEquals("D1234AB", result.getNoPolisi());
            assertNotNull(result.getSpkDetails());
            assertEquals(1, result.getSpkDetails().size());
        }

        @Test
        @DisplayName("handles null SPK details gracefully")
        void handlesNullSpkDetails() {
            TbPenjualanEntity entity = new TbPenjualanEntity();
            entity.setNoPenjualan("PJ003");
            entity.setNoSpk("SPK003");
            entity.setPelangganId(null);

            TbSpkEntity spkEntity = new TbSpkEntity();
            spkEntity.setPelangganId(3L);

            TbPelangganEntity pelanggan = new TbPelangganEntity();
            pelanggan.setNamaPelanggan("Ani");

            when(repository.findByNoPenjualan("PJ003")).thenReturn(entity);
            when(spkService.findByNoSpk("SPK003")).thenReturn(spkEntity);
            when(spkDetailService.findByNoSpk("SPK003")).thenReturn(null);
            when(pelangganService.findById(3L)).thenReturn(pelanggan);

            TbPenjualanEntity result = penjualanService.findByNoPenjualan("PJ003");

            assertNotNull(result);
            assertEquals("Ani", result.getNamaPelanggan());
            assertNull(result.getSpkDetails());
        }

        @Test
        @DisplayName("handles pelanggan not found after SPK lookup")
        void pelangganNotFoundAfterSpk() {
            TbPenjualanEntity entity = new TbPenjualanEntity();
            entity.setNoPenjualan("PJ004");
            entity.setNoSpk("SPK004");
            entity.setPelangganId(null);

            TbSpkEntity spkEntity = new TbSpkEntity();
            spkEntity.setPelangganId(999L);

            when(repository.findByNoPenjualan("PJ004")).thenReturn(entity);
            when(spkService.findByNoSpk("SPK004")).thenReturn(spkEntity);
            when(spkDetailService.findByNoSpk("SPK004")).thenReturn(List.of());
            when(pelangganService.findById(999L)).thenReturn(null);

            TbPenjualanEntity result = penjualanService.findByNoPenjualan("PJ004");

            assertNotNull(result);
            assertNull(result.getNamaPelanggan());
            assertEquals(0, result.getSpkDetails().size());
        }

        @Test
        @DisplayName("handles SPK not found when pelangganId is null")
        void spkNotFoundWhenPelangganIdNull() {
            TbPenjualanEntity entity = new TbPenjualanEntity();
            entity.setNoPenjualan("PJ005");
            entity.setNoSpk("SPK005");
            entity.setPelangganId(null);

            when(repository.findByNoPenjualan("PJ005")).thenReturn(entity);
            when(spkService.findByNoSpk("SPK005")).thenReturn(null);
            when(spkDetailService.findByNoSpk("SPK005")).thenReturn(List.of());

            TbPenjualanEntity result = penjualanService.findByNoPenjualan("PJ005");

            assertNotNull(result);
            assertNull(result.getNamaPelanggan());
        }

        @Test
        @DisplayName("handles pelanggan found but fields null")
        void pelangganFieldsNull() {
            TbPenjualanEntity entity = new TbPenjualanEntity();
            entity.setNoPenjualan("PJ006");
            entity.setNoSpk("SPK006");
            entity.setPelangganId(null);

            TbSpkEntity spkEntity = new TbSpkEntity();
            spkEntity.setPelangganId(6L);

            TbPelangganEntity pelanggan = new TbPelangganEntity();
            pelanggan.setNamaPelanggan(null);
            pelanggan.setAlamat(null);

            when(repository.findByNoPenjualan("PJ006")).thenReturn(entity);
            when(spkService.findByNoSpk("SPK006")).thenReturn(spkEntity);
            when(spkDetailService.findByNoSpk("SPK006")).thenReturn(List.of());
            when(pelangganService.findById(6L)).thenReturn(pelanggan);

            TbPenjualanEntity result = penjualanService.findByNoPenjualan("PJ006");

            assertNotNull(result);
            assertNull(result.getNamaPelanggan());
            assertNull(result.getAlamatPelanggan());
        }
    }

    // ─── buildPrintDto ────────────────────────────────────────────────

    @Nested
    @DisplayName("buildPrintDto")
    class BuildPrintDto {

        @Test
        @DisplayName("returns null when penjualan not found")
        void returnsNull() {
            PanacheQuery<TbPenjualanEntity> query = mock(PanacheQuery.class);
            when(repository.find(eq("noPenjualan"), eq("NONE"))).thenReturn(query);
            when(query.firstResult()).thenReturn(null);

            assertNull(penjualanService.buildPrintDto("NONE"));
        }

        @Test
        @DisplayName("basic DTO without SPK")
        void basicDtoWithoutSpk() {
            testEntity.setNoSpk(null);

            PanacheQuery<TbPenjualanEntity> query = mock(PanacheQuery.class);
            when(repository.find(eq("noPenjualan"), eq("PJ001"))).thenReturn(query);
            when(query.firstResult()).thenReturn(testEntity);
            when(pelangganService.findById(1L)).thenReturn(null);

            PenjualanPrintDto dto = penjualanService.buildPrintDto("PJ001");

            assertNotNull(dto);
            assertEquals("PJ001", dto.getNoPenjualan());
            assertNull(dto.getNoSpk());
            assertEquals(testEntity.getStatusPembayaran(), dto.getStatusPembayaran());
            assertEquals(testEntity.getMetodePembayaran(), dto.getMetodePembayaran());
            assertEquals(testEntity.getDiscount(), dto.getDiskon());
            assertEquals(testEntity.getGrandTotal(), dto.getGrandTotal());
            assertEquals(testEntity.getUangDibayar(), dto.getUangDibayar());
            assertEquals(testEntity.getKembalian(), dto.getKembalian());
        }

        @Test
        @DisplayName("with SPK and kendaraanId set — looks up kendaraan")
        void withSpkAndKendaraanId() {
            TbSpkEntity spk = new TbSpkEntity();
            spk.setId(10L);
            spk.setPelangganId(1L);
            spk.setKmSaatIni(50000);
            spk.setNopol("B1234CD");

            TbKendaraanEntity kendaraan = new TbKendaraanEntity();
            kendaraan.setMerk("Toyota");
            kendaraan.setModel("Avanza");

            TbPelangganEntity pelanggan = new TbPelangganEntity();
            pelanggan.setNamaPelanggan("Andi");
            pelanggan.setAlamat("Jakarta");
            pelanggan.setNoHp("08123456789");

            PanacheQuery<TbPenjualanEntity> pQuery = mock(PanacheQuery.class);
            when(repository.find(eq("noPenjualan"), eq("PJ001"))).thenReturn(pQuery);
            when(pQuery.firstResult()).thenReturn(testEntity);

            PanacheQuery<TbSpkEntity> spkQuery = mock(PanacheQuery.class);
            when(spkRepository.find(eq("noSpk"), eq("SPK001"))).thenReturn(spkQuery);
            when(spkQuery.firstResult()).thenReturn(spk);

            when(pelangganService.findById(1L)).thenReturn(pelanggan);
            when(kendaraanRepository.findById(1L)).thenReturn(kendaraan);
            when(spkService.findById(10L)).thenReturn(spk);

            PenjualanPrintDto dto = penjualanService.buildPrintDto("PJ001");

            assertNotNull(dto);
            assertEquals("Andi", dto.getNamaPelanggan());
            assertEquals("Jakarta", dto.getAlamatPelanggan());
            assertEquals("08123456789", dto.getNoHpPelanggan());
            assertEquals(Integer.valueOf(50000), dto.getKm());
            assertEquals("B1234CD", dto.getNopol());
            assertEquals("Toyota", dto.getMerk());
            assertEquals("Avanza", dto.getModel());
        }

        @Test
        @DisplayName("with SPK, no kendaraanId, but nopol — looks up pelanggan by nopol")
        void withSpkNopolFallback() {
            testEntity.setKendaraanId(null);

            TbSpkEntity spk = new TbSpkEntity();
            spk.setId(10L);
            spk.setPelangganId(1L);
            spk.setNopol("D5678EF");
            spk.setKmSaatIni(30000);

            TbPelangganEntity pelangganByLookup = new TbPelangganEntity();
            pelangganByLookup.setMerk("Honda");
            pelangganByLookup.setJenis("Beat");

            PanacheQuery<TbPenjualanEntity> pQuery = mock(PanacheQuery.class);
            when(repository.find(eq("noPenjualan"), eq("PJ001"))).thenReturn(pQuery);
            when(pQuery.firstResult()).thenReturn(testEntity);

            PanacheQuery<TbSpkEntity> spkQuery = mock(PanacheQuery.class);
            when(spkRepository.find(eq("noSpk"), eq("SPK001"))).thenReturn(spkQuery);
            when(spkQuery.firstResult()).thenReturn(spk);

            when(pelangganService.findById(1L)).thenReturn(null);
            when(pelangganService.findByNopol("D5678EF")).thenReturn(pelangganByLookup);
            when(spkService.findById(10L)).thenReturn(spk);

            PenjualanPrintDto dto = penjualanService.buildPrintDto("PJ001");

            assertNotNull(dto);
            assertEquals("D5678EF", dto.getNopol());
            assertEquals("Honda", dto.getMerk());
            assertEquals("Beat", dto.getModel());
        }

        @Test
        @DisplayName("with SPK, no kendaraanId, nopol set, but pelanggan not found by nopol")
        void withSpkNopolPelangganNotFound() {
            testEntity.setKendaraanId(null);

            TbSpkEntity spk = new TbSpkEntity();
            spk.setId(10L);
            spk.setPelangganId(1L);
            spk.setNopol("X9999ZZ");
            spk.setKmSaatIni(1000);

            PanacheQuery<TbPenjualanEntity> pQuery = mock(PanacheQuery.class);
            when(repository.find(eq("noPenjualan"), eq("PJ001"))).thenReturn(pQuery);
            when(pQuery.firstResult()).thenReturn(testEntity);

            PanacheQuery<TbSpkEntity> spkQuery = mock(PanacheQuery.class);
            when(spkRepository.find(eq("noSpk"), eq("SPK001"))).thenReturn(spkQuery);
            when(spkQuery.firstResult()).thenReturn(spk);

            when(pelangganService.findById(1L)).thenReturn(null);
            when(pelangganService.findByNopol("X9999ZZ")).thenReturn(null);
            when(spkService.findById(10L)).thenReturn(spk);

            PenjualanPrintDto dto = penjualanService.buildPrintDto("PJ001");

            assertNotNull(dto);
            assertEquals("X9999ZZ", dto.getNopol());
            assertNull(dto.getMerk());
            assertNull(dto.getModel());
        }

        @Test
        @DisplayName("with SPK, kendaraanId set, but kendaraan not found")
        void kendaraanNotFound() {
            TbSpkEntity spk = new TbSpkEntity();
            spk.setId(10L);
            spk.setPelangganId(1L);
            spk.setKmSaatIni(50000);

            PanacheQuery<TbPenjualanEntity> pQuery = mock(PanacheQuery.class);
            when(repository.find(eq("noPenjualan"), eq("PJ001"))).thenReturn(pQuery);
            when(pQuery.firstResult()).thenReturn(testEntity);

            PanacheQuery<TbSpkEntity> spkQuery = mock(PanacheQuery.class);
            when(spkRepository.find(eq("noSpk"), eq("SPK001"))).thenReturn(spkQuery);
            when(spkQuery.firstResult()).thenReturn(spk);

            when(pelangganService.findById(1L)).thenReturn(null);
            when(kendaraanRepository.findById(1L)).thenReturn(null);
            when(spkService.findById(10L)).thenReturn(spk);

            PenjualanPrintDto dto = penjualanService.buildPrintDto("PJ001");

            assertNotNull(dto);
            assertNull(dto.getMerk());
            assertNull(dto.getModel());
        }

        @Test
        @DisplayName("with SPK containing mekanikList — resolves karyawan names")
        void withMekanikList() {
            TbSpkEntity spk = new TbSpkEntity();
            spk.setId(10L);
            spk.setPelangganId(1L);
            SpkMekanik m1 = new SpkMekanik();
            m1.setId(100L);
            m1.setTugas("Servis");
            SpkMekanik m2 = new SpkMekanik();
            m2.setId(200L);
            m2.setTugas("Ganti Oli");
            spk.setMekanikList(List.of(m1, m2));

            TbKaryawanEntity k1 = new TbKaryawanEntity();
            k1.setNamaKaryawan("Siti");
            TbKaryawanEntity k2 = new TbKaryawanEntity();
            k2.setNamaKaryawan("Budi");

            PanacheQuery<TbPenjualanEntity> pQuery = mock(PanacheQuery.class);
            when(repository.find(eq("noPenjualan"), eq("PJ001"))).thenReturn(pQuery);
            when(pQuery.firstResult()).thenReturn(testEntity);

            PanacheQuery<TbSpkEntity> spkQuery = mock(PanacheQuery.class);
            when(spkRepository.find(eq("noSpk"), eq("SPK001"))).thenReturn(spkQuery);
            when(spkQuery.firstResult()).thenReturn(spk);

            when(pelangganService.findById(1L)).thenReturn(null);

            @SuppressWarnings("unchecked")
            PanacheQuery<TbKaryawanEntity> karyawanQuery = mock(PanacheQuery.class);
            when(karyawanRepository.find(eq("id in ?1"), eq(List.of(100L, 200L))))
                    .thenReturn(karyawanQuery);
            when(karyawanQuery.stream()).thenReturn(java.util.stream.Stream.of(k1, k2));

            when(spkService.findById(10L)).thenReturn(spk);

            PenjualanPrintDto dto = penjualanService.buildPrintDto("PJ001");

            assertNotNull(dto);
            assertEquals("Siti, Budi", dto.getNamaMekanik());
        }

        @Test
        @DisplayName("with SPK, empty mekanikList — no namaMekanik set")
        void withEmptyMekanikList() {
            TbSpkEntity spk = new TbSpkEntity();
            spk.setId(10L);
            spk.setPelangganId(1L);
            spk.setMekanikList(List.of());

            PanacheQuery<TbPenjualanEntity> pQuery = mock(PanacheQuery.class);
            when(repository.find(eq("noPenjualan"), eq("PJ001"))).thenReturn(pQuery);
            when(pQuery.firstResult()).thenReturn(testEntity);

            PanacheQuery<TbSpkEntity> spkQuery = mock(PanacheQuery.class);
            when(spkRepository.find(eq("noSpk"), eq("SPK001"))).thenReturn(spkQuery);
            when(spkQuery.firstResult()).thenReturn(spk);

            when(pelangganService.findById(1L)).thenReturn(null);
            when(spkService.findById(10L)).thenReturn(spk);

            PenjualanPrintDto dto = penjualanService.buildPrintDto("PJ001");

            assertNotNull(dto);
            assertNull(dto.getNamaMekanik());
        }

        @Test
        @DisplayName("with SPK, null mekanikList — no namaMekanik set")
        void withNullMekanikList() {
            TbSpkEntity spk = new TbSpkEntity();
            spk.setId(10L);
            spk.setPelangganId(1L);
            spk.setMekanikList(null);

            PanacheQuery<TbPenjualanEntity> pQuery = mock(PanacheQuery.class);
            when(repository.find(eq("noPenjualan"), eq("PJ001"))).thenReturn(pQuery);
            when(pQuery.firstResult()).thenReturn(testEntity);

            PanacheQuery<TbSpkEntity> spkQuery = mock(PanacheQuery.class);
            when(spkRepository.find(eq("noSpk"), eq("SPK001"))).thenReturn(spkQuery);
            when(spkQuery.firstResult()).thenReturn(spk);

            when(pelangganService.findById(1L)).thenReturn(null);
            when(spkService.findById(10L)).thenReturn(spk);

            PenjualanPrintDto dto = penjualanService.buildPrintDto("PJ001");

            assertNull(dto.getNamaMekanik());
        }

        @Test
        @DisplayName("with SPK details containing jasa item")
        void withJasaDetail() {
            TbSpkEntity spk = new TbSpkEntity();
            spk.setId(10L);
            spk.setPelangganId(1L);

            TbSpkDetailEntity detail = new TbSpkDetailEntity();
            detail.setJasaId(50L);
            detail.setSparepartId(null);
            detail.setJumlah(2);

            TbSpkEntity fullSpk = new TbSpkEntity();
            fullSpk.setDetails(List.of(detail));

            TbJasaEntity jasa = new TbJasaEntity();
            jasa.setHargaJasa(150000);

            PanacheQuery<TbPenjualanEntity> pQuery = mock(PanacheQuery.class);
            when(repository.find(eq("noPenjualan"), eq("PJ001"))).thenReturn(pQuery);
            when(pQuery.firstResult()).thenReturn(testEntity);

            PanacheQuery<TbSpkEntity> spkQuery = mock(PanacheQuery.class);
            when(spkRepository.find(eq("noSpk"), eq("SPK001"))).thenReturn(spkQuery);
            when(spkQuery.firstResult()).thenReturn(spk);

            when(pelangganService.findById(1L)).thenReturn(null);
            when(spkService.findById(10L)).thenReturn(fullSpk);
            when(jasaRepository.findById(50L)).thenReturn(jasa);

            PenjualanPrintDto dto = penjualanService.buildPrintDto("PJ001");

            assertNotNull(dto);
            assertEquals(1, dto.getItems().size());
            PenjualanPrintDto.ItemDto item = dto.getItems().get(0);
            assertEquals(Integer.valueOf(2), item.getQty());
            assertEquals(BigDecimal.valueOf(150000), item.getHarga());
            assertEquals("JASA", item.getType());
            assertEquals(BigDecimal.valueOf(300000), item.getSubTotal());
            assertEquals(BigDecimal.valueOf(300000), dto.getSubTotal());
        }

        @Test
        @DisplayName("with SPK details containing sparepart item")
        void withSparepartDetail() {
            TbSpkEntity spk = new TbSpkEntity();
            spk.setId(10L);
            spk.setPelangganId(1L);

            TbSpkDetailEntity detail = new TbSpkDetailEntity();
            detail.setJasaId(null);
            detail.setSparepartId(60L);
            detail.setJumlah(3);

            TbSpkEntity fullSpk = new TbSpkEntity();
            fullSpk.setDetails(List.of(detail));

            TbSparepartEntity sparepart = new TbSparepartEntity();
            sparepart.setHargaJual(BigDecimal.valueOf(50000));

            PanacheQuery<TbPenjualanEntity> pQuery = mock(PanacheQuery.class);
            when(repository.find(eq("noPenjualan"), eq("PJ001"))).thenReturn(pQuery);
            when(pQuery.firstResult()).thenReturn(testEntity);

            PanacheQuery<TbSpkEntity> spkQuery = mock(PanacheQuery.class);
            when(spkRepository.find(eq("noSpk"), eq("SPK001"))).thenReturn(spkQuery);
            when(spkQuery.firstResult()).thenReturn(spk);

            when(pelangganService.findById(1L)).thenReturn(null);
            when(spkService.findById(10L)).thenReturn(fullSpk);
            when(sparepartRepository.findById(60L)).thenReturn(sparepart);

            PenjualanPrintDto dto = penjualanService.buildPrintDto("PJ001");

            assertNotNull(dto);
            PenjualanPrintDto.ItemDto item = dto.getItems().get(0);
            assertEquals(BigDecimal.valueOf(50000), item.getHarga());
            assertEquals("BARANG", item.getType());
            assertEquals(BigDecimal.valueOf(150000), item.getSubTotal());
        }

        @Test
        @DisplayName("with SPK details — sparepart not found, falls back to barang")
        void sparepartNotFoundFallbackToBarang() {
            TbSpkEntity spk = new TbSpkEntity();
            spk.setId(10L);
            spk.setPelangganId(1L);

            TbSpkDetailEntity detail = new TbSpkDetailEntity();
            detail.setJasaId(null);
            detail.setSparepartId(70L);
            detail.setJumlah(1);

            TbSpkEntity fullSpk = new TbSpkEntity();
            fullSpk.setDetails(List.of(detail));

            TbBarangEntity barang = new TbBarangEntity();
            barang.setHargaJual(BigDecimal.valueOf(200000));

            PanacheQuery<TbPenjualanEntity> pQuery = mock(PanacheQuery.class);
            when(repository.find(eq("noPenjualan"), eq("PJ001"))).thenReturn(pQuery);
            when(pQuery.firstResult()).thenReturn(testEntity);

            PanacheQuery<TbSpkEntity> spkQuery = mock(PanacheQuery.class);
            when(spkRepository.find(eq("noSpk"), eq("SPK001"))).thenReturn(spkQuery);
            when(spkQuery.firstResult()).thenReturn(spk);

            when(pelangganService.findById(1L)).thenReturn(null);
            when(spkService.findById(10L)).thenReturn(fullSpk);
            when(sparepartRepository.findById(70L)).thenReturn(null);
            when(barangRepository.findById(70L)).thenReturn(barang);

            PenjualanPrintDto dto = penjualanService.buildPrintDto("PJ001");

            assertNotNull(dto);
            PenjualanPrintDto.ItemDto item = dto.getItems().get(0);
            assertEquals(BigDecimal.valueOf(200000), item.getHarga());
            assertEquals("BARANG", item.getType());
        }

        @Test
        @DisplayName("with SPK details — neither sparepart nor barang found")
        void sparepartAndBarangNotFound() {
            TbSpkEntity spk = new TbSpkEntity();
            spk.setId(10L);
            spk.setPelangganId(1L);

            TbSpkDetailEntity detail = new TbSpkDetailEntity();
            detail.setJasaId(null);
            detail.setSparepartId(80L);
            detail.setJumlah(5);

            TbSpkEntity fullSpk = new TbSpkEntity();
            fullSpk.setDetails(List.of(detail));

            PanacheQuery<TbPenjualanEntity> pQuery = mock(PanacheQuery.class);
            when(repository.find(eq("noPenjualan"), eq("PJ001"))).thenReturn(pQuery);
            when(pQuery.firstResult()).thenReturn(testEntity);

            PanacheQuery<TbSpkEntity> spkQuery = mock(PanacheQuery.class);
            when(spkRepository.find(eq("noSpk"), eq("SPK001"))).thenReturn(spkQuery);
            when(spkQuery.firstResult()).thenReturn(spk);

            when(pelangganService.findById(1L)).thenReturn(null);
            when(spkService.findById(10L)).thenReturn(fullSpk);
            when(sparepartRepository.findById(80L)).thenReturn(null);
            when(barangRepository.findById(80L)).thenReturn(null);

            PenjualanPrintDto dto = penjualanService.buildPrintDto("PJ001");

            assertNotNull(dto);
            PenjualanPrintDto.ItemDto item = dto.getItems().get(0);
            assertEquals(BigDecimal.ZERO, item.getHarga());
            assertEquals("UNKNOWN", item.getType());
            assertEquals(BigDecimal.ZERO, item.getSubTotal());
        }

        @Test
        @DisplayName("with SPK details containing jasaId but jasa not found")
        void jasaNotFound() {
            TbSpkEntity spk = new TbSpkEntity();
            spk.setId(10L);
            spk.setPelangganId(1L);

            TbSpkDetailEntity detail = new TbSpkDetailEntity();
            detail.setJasaId(99L);
            detail.setSparepartId(null);
            detail.setJumlah(1);

            TbSpkEntity fullSpk = new TbSpkEntity();
            fullSpk.setDetails(List.of(detail));

            PanacheQuery<TbPenjualanEntity> pQuery = mock(PanacheQuery.class);
            when(repository.find(eq("noPenjualan"), eq("PJ001"))).thenReturn(pQuery);
            when(pQuery.firstResult()).thenReturn(testEntity);

            PanacheQuery<TbSpkEntity> spkQuery = mock(PanacheQuery.class);
            when(spkRepository.find(eq("noSpk"), eq("SPK001"))).thenReturn(spkQuery);
            when(spkQuery.firstResult()).thenReturn(spk);

            when(pelangganService.findById(1L)).thenReturn(null);
            when(spkService.findById(10L)).thenReturn(fullSpk);
            when(jasaRepository.findById(99L)).thenReturn(null);

            PenjualanPrintDto dto = penjualanService.buildPrintDto("PJ001");

            assertNotNull(dto);
            PenjualanPrintDto.ItemDto item = dto.getItems().get(0);
            assertEquals(BigDecimal.ZERO, item.getHarga());
            assertEquals("UNKNOWN", item.getType());
        }

        @Test
        @DisplayName("with SPK details — detail has both jasaId and sparepartId (jasa takes precedence)")
        void jasaTakesPrecedenceOverSparepart() {
            TbSpkEntity spk = new TbSpkEntity();
            spk.setId(10L);
            spk.setPelangganId(1L);

            TbSpkDetailEntity detail = new TbSpkDetailEntity();
            detail.setJasaId(50L);
            detail.setSparepartId(60L);
            detail.setJumlah(1);

            TbSpkEntity fullSpk = new TbSpkEntity();
            fullSpk.setDetails(List.of(detail));

            TbJasaEntity jasa = new TbJasaEntity();
            jasa.setHargaJasa(100000);

            PanacheQuery<TbPenjualanEntity> pQuery = mock(PanacheQuery.class);
            when(repository.find(eq("noPenjualan"), eq("PJ001"))).thenReturn(pQuery);
            when(pQuery.firstResult()).thenReturn(testEntity);

            PanacheQuery<TbSpkEntity> spkQuery = mock(PanacheQuery.class);
            when(spkRepository.find(eq("noSpk"), eq("SPK001"))).thenReturn(spkQuery);
            when(spkQuery.firstResult()).thenReturn(spk);

            when(pelangganService.findById(1L)).thenReturn(null);
            when(spkService.findById(10L)).thenReturn(fullSpk);
            when(jasaRepository.findById(50L)).thenReturn(jasa);

            PenjualanPrintDto dto = penjualanService.buildPrintDto("PJ001");

            PenjualanPrintDto.ItemDto item = dto.getItems().get(0);
            assertEquals(BigDecimal.valueOf(100000), item.getHarga());
            assertEquals("JASA", item.getType());
            verifyNoInteractions(sparepartRepository, barangRepository);
        }

        @Test
        @DisplayName("with SPK but fullSpk returns null")
        void fullSpkReturnsNull() {
            TbSpkEntity spk = new TbSpkEntity();
            spk.setId(10L);
            spk.setPelangganId(1L);

            PanacheQuery<TbPenjualanEntity> pQuery = mock(PanacheQuery.class);
            when(repository.find(eq("noPenjualan"), eq("PJ001"))).thenReturn(pQuery);
            when(pQuery.firstResult()).thenReturn(testEntity);

            PanacheQuery<TbSpkEntity> spkQuery = mock(PanacheQuery.class);
            when(spkRepository.find(eq("noSpk"), eq("SPK001"))).thenReturn(spkQuery);
            when(spkQuery.firstResult()).thenReturn(spk);

            when(pelangganService.findById(1L)).thenReturn(null);
            when(spkService.findById(10L)).thenReturn(null);

            PenjualanPrintDto dto = penjualanService.buildPrintDto("PJ001");

            assertNotNull(dto);
            assertNotNull(dto.getItems());
            assertEquals(0, dto.getItems().size());
            assertEquals(BigDecimal.ZERO, dto.getSubTotal());
        }

        @Test
        @DisplayName("with SPK but fullSpk has null details")
        void fullSpkNullDetails() {
            TbSpkEntity spk = new TbSpkEntity();
            spk.setId(10L);
            spk.setPelangganId(1L);

            TbSpkEntity fullSpk = new TbSpkEntity();
            fullSpk.setDetails(null);

            PanacheQuery<TbPenjualanEntity> pQuery = mock(PanacheQuery.class);
            when(repository.find(eq("noPenjualan"), eq("PJ001"))).thenReturn(pQuery);
            when(pQuery.firstResult()).thenReturn(testEntity);

            PanacheQuery<TbSpkEntity> spkQuery = mock(PanacheQuery.class);
            when(spkRepository.find(eq("noSpk"), eq("SPK001"))).thenReturn(spkQuery);
            when(spkQuery.firstResult()).thenReturn(spk);

            when(pelangganService.findById(1L)).thenReturn(null);
            when(spkService.findById(10L)).thenReturn(fullSpk);

            PenjualanPrintDto dto = penjualanService.buildPrintDto("PJ001");

            assertNotNull(dto);
            assertEquals(0, dto.getItems().size());
        }

        @Test
        @DisplayName("with multiple SPK details — aggregates subTotal correctly")
        void multipleDetailsAggregateSubTotal() {
            TbSpkEntity spk = new TbSpkEntity();
            spk.setId(10L);
            spk.setPelangganId(1L);

            TbSpkDetailEntity d1 = new TbSpkDetailEntity();
            d1.setJasaId(50L);
            d1.setSparepartId(null);
            d1.setJumlah(2);

            TbSpkDetailEntity d2 = new TbSpkDetailEntity();
            d2.setJasaId(null);
            d2.setSparepartId(60L);
            d2.setJumlah(1);

            TbSpkEntity fullSpk = new TbSpkEntity();
            fullSpk.setDetails(List.of(d1, d2));

            TbJasaEntity jasa = new TbJasaEntity();
            jasa.setHargaJasa(100000);
            TbSparepartEntity sparepart = new TbSparepartEntity();
            sparepart.setHargaJual(BigDecimal.valueOf(250000));

            PanacheQuery<TbPenjualanEntity> pQuery = mock(PanacheQuery.class);
            when(repository.find(eq("noPenjualan"), eq("PJ001"))).thenReturn(pQuery);
            when(pQuery.firstResult()).thenReturn(testEntity);

            PanacheQuery<TbSpkEntity> spkQuery = mock(PanacheQuery.class);
            when(spkRepository.find(eq("noSpk"), eq("SPK001"))).thenReturn(spkQuery);
            when(spkQuery.firstResult()).thenReturn(spk);

            when(pelangganService.findById(1L)).thenReturn(null);
            when(spkService.findById(10L)).thenReturn(fullSpk);
            when(jasaRepository.findById(50L)).thenReturn(jasa);
            when(sparepartRepository.findById(60L)).thenReturn(sparepart);

            PenjualanPrintDto dto = penjualanService.buildPrintDto("PJ001");

            assertEquals(2, dto.getItems().size());
            // d1: 2 * 100000 = 200000, d2: 1 * 250000 = 250000 → subTotal = 450000
            assertEquals(BigDecimal.valueOf(450000), dto.getSubTotal());
        }

        @Test
        @DisplayName("pelangganId from SPK used when penjualan pelangganId is null")
        void pelangganIdFromSpk() {
            testEntity.setPelangganId(null);

            TbSpkEntity spk = new TbSpkEntity();
            spk.setId(10L);
            spk.setPelangganId(42L);

            TbPelangganEntity pelanggan = new TbPelangganEntity();
            pelanggan.setNamaPelanggan("Rina");
            pelanggan.setAlamat("Surabaya");
            pelanggan.setNoHp("0811112222");

            PanacheQuery<TbPenjualanEntity> pQuery = mock(PanacheQuery.class);
            when(repository.find(eq("noPenjualan"), eq("PJ001"))).thenReturn(pQuery);
            when(pQuery.firstResult()).thenReturn(testEntity);

            PanacheQuery<TbSpkEntity> spkQuery = mock(PanacheQuery.class);
            when(spkRepository.find(eq("noSpk"), eq("SPK001"))).thenReturn(spkQuery);
            when(spkQuery.firstResult()).thenReturn(spk);

            when(pelangganService.findById(42L)).thenReturn(pelanggan);
            when(spkService.findById(10L)).thenReturn(spk);

            PenjualanPrintDto dto = penjualanService.buildPrintDto("PJ001");

            assertEquals("Rina", dto.getNamaPelanggan());
            assertEquals("Surabaya", dto.getAlamatPelanggan());
            assertEquals("0811112222", dto.getNoHpPelanggan());
        }

        @Test
        @DisplayName("SPK nopol is null — skips nopol/kendaraan lookup")
        void spkNopolIsNull() {
            testEntity.setKendaraanId(null);

            TbSpkEntity spk = new TbSpkEntity();
            spk.setId(10L);
            spk.setPelangganId(1L);
            spk.setNopol(null);

            PanacheQuery<TbPenjualanEntity> pQuery = mock(PanacheQuery.class);
            when(repository.find(eq("noPenjualan"), eq("PJ001"))).thenReturn(pQuery);
            when(pQuery.firstResult()).thenReturn(testEntity);

            PanacheQuery<TbSpkEntity> spkQuery = mock(PanacheQuery.class);
            when(spkRepository.find(eq("noSpk"), eq("SPK001"))).thenReturn(spkQuery);
            when(spkQuery.firstResult()).thenReturn(spk);

            when(pelangganService.findById(1L)).thenReturn(null);
            when(spkService.findById(10L)).thenReturn(spk);

            PenjualanPrintDto dto = penjualanService.buildPrintDto("PJ001");

            assertNull(dto.getNopol());
            assertNull(dto.getMerk());
            assertNull(dto.getModel());
        }
    }

    // ─── createWithNoSpkValidation ────────────────────────────────────

    @Nested
    @DisplayName("createWithNoSpkValidation")
    class CreateWithNoSpkValidation {

        @Test
        @DisplayName("succeeds when no duplicate exists")
        void succeeds() {
            PanacheQuery<TbPenjualanEntity> findQuery = mock(PanacheQuery.class);
            when(repository.find(eq("noSpk"), any(Object[].class))).thenReturn(findQuery);
            when(findQuery.firstResult()).thenReturn(null);

            TbPenjualanEntity result = penjualanService.createWithNoSpkValidation(testEntity);

            assertNotNull(result);
            verify(repository).persist(testEntity);
        }

        @Test
        @DisplayName("throws when SPK already exists in penjualan")
        void throwsOnDuplicate() {
            PanacheQuery<TbPenjualanEntity> findQuery = mock(PanacheQuery.class);
            when(repository.find(eq("noSpk"), any(Object[].class))).thenReturn(findQuery);
            when(findQuery.firstResult()).thenReturn(testEntity);

            TbPenjualanEntity entity = new TbPenjualanEntity();
            entity.setNoSpk("SPK001");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> penjualanService.createWithNoSpkValidation(entity));
            assertTrue(ex.getMessage().contains("SPK001"));
            verify(repository, never()).persist((TbPenjualanEntity) any());
        }
    }

    // ─── updateWithNoSpkValidation ────────────────────────────────────

    @Nested
    @DisplayName("updateWithNoSpkValidation")
    class UpdateWithNoSpkValidation {

        @Test
        @DisplayName("succeeds when no duplicate exists")
        void succeeds() {
            PanacheQuery<TbPenjualanEntity> findQuery = mock(PanacheQuery.class);
            when(repository.find(eq("noSpk = ?1 AND noPenjualan <> ?2"), any(Object[].class)))
                    .thenReturn(findQuery);
            when(findQuery.firstResult()).thenReturn(null);
            when(repository.findByIdOptional("PJ001")).thenReturn(Optional.of(testEntity));
            EntityManager em = mock(EntityManager.class);
            when(repository.getEntityManager()).thenReturn(em);
            when(em.merge(any(TbPenjualanEntity.class))).thenReturn(testEntity);

            TbPenjualanEntity result = penjualanService.updateWithNoSpkValidation(testEntity);

            assertNotNull(result);
            verify(em).merge(testEntity);
        }

        @Test
        @DisplayName("throws when duplicate SPK exists in other penjualan")
        void throwsOnDuplicate() {
            TbPenjualanEntity otherEntity = new TbPenjualanEntity();
            otherEntity.setNoPenjualan("PJ999");

            PanacheQuery<TbPenjualanEntity> findQuery = mock(PanacheQuery.class);
            when(repository.find(eq("noSpk = ?1 AND noPenjualan <> ?2"), any(Object[].class)))
                    .thenReturn(findQuery);
            when(findQuery.firstResult()).thenReturn(otherEntity);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> penjualanService.updateWithNoSpkValidation(testEntity));
            assertTrue(ex.getMessage().contains("SPK001"));
        }
    }

    // ─── findPaginated ────────────────────────────────────────────────

    @Nested
    @DisplayName("findPaginated")
    class FindPaginated {

        @Test
        @DisplayName("without any filters")
        void noFilters() {
            PageRequest pr = new PageRequest(1, 10);
            when(repository.find(anyString(), any(Parameters.class))).thenReturn(panacheQuery);
            when(panacheQuery.count()).thenReturn(1L);
            when(panacheQuery.page(any())).thenReturn(panacheQuery);
            when(panacheQuery.list()).thenReturn(List.of(testEntity));

            PageResponse<TbPenjualanEntity> result = penjualanService.findPaginated(pr);

            assertEquals(1, result.getRowsNumber());
            assertEquals(1, result.getRows().size());
        }

        @Test
        @DisplayName("with search filter")
        void withSearch() {
            PageRequest pr = new PageRequest(1, 10);
            pr.setSearch("PJ");
            when(repository.find(anyString(), any(Parameters.class))).thenReturn(panacheQuery);
            when(panacheQuery.count()).thenReturn(2L);
            when(panacheQuery.page(any())).thenReturn(panacheQuery);
            when(panacheQuery.list()).thenReturn(List.of(testEntity));

            PageResponse<TbPenjualanEntity> result = penjualanService.findPaginated(pr);

            assertEquals(2, result.getRowsNumber());
        }

        @Test
        @DisplayName("with status filter (single status)")
        void withSingleStatusFilter() {
            PageRequest pr = new PageRequest(1, 10);
            pr.setStatusFilter("LUNAS");
            when(repository.find(anyString(), any(Parameters.class))).thenReturn(panacheQuery);
            when(panacheQuery.count()).thenReturn(1L);
            when(panacheQuery.page(any())).thenReturn(panacheQuery);
            when(panacheQuery.list()).thenReturn(List.of(testEntity));

            PageResponse<TbPenjualanEntity> result = penjualanService.findPaginated(pr);

            assertEquals(1, result.getRowsNumber());
        }

        @Test
        @DisplayName("with status filter (multiple statuses comma-separated)")
        void withMultipleStatusFilter() {
            PageRequest pr = new PageRequest(1, 10);
            pr.setStatusFilter("LUNAS,BELUM_LUNAS");
            when(repository.find(anyString(), any(Parameters.class))).thenReturn(panacheQuery);
            when(panacheQuery.count()).thenReturn(3L);
            when(panacheQuery.page(any())).thenReturn(panacheQuery);
            when(panacheQuery.list()).thenReturn(List.of(testEntity));

            PageResponse<TbPenjualanEntity> result = penjualanService.findPaginated(pr);

            assertEquals(3, result.getRowsNumber());
        }

        @Test
        @DisplayName("with startDate filter")
        void withStartDate() {
            PageRequest pr = new PageRequest(1, 10);
            pr.setStartDate("2024-01-01");
            when(repository.find(anyString(), any(Parameters.class))).thenReturn(panacheQuery);
            when(panacheQuery.count()).thenReturn(5L);
            when(panacheQuery.page(any())).thenReturn(panacheQuery);
            when(panacheQuery.list()).thenReturn(List.of(testEntity));

            PageResponse<TbPenjualanEntity> result = penjualanService.findPaginated(pr);

            assertEquals(5, result.getRowsNumber());
        }

        @Test
        @DisplayName("with endDate filter")
        void withEndDate() {
            PageRequest pr = new PageRequest(1, 10);
            pr.setEndDate("2024-12-31");
            when(repository.find(anyString(), any(Parameters.class))).thenReturn(panacheQuery);
            when(panacheQuery.count()).thenReturn(10L);
            when(panacheQuery.page(any())).thenReturn(panacheQuery);
            when(panacheQuery.list()).thenReturn(List.of(testEntity));

            PageResponse<TbPenjualanEntity> result = penjualanService.findPaginated(pr);

            assertEquals(10, result.getRowsNumber());
        }

        @Test
        @DisplayName("with all filters combined")
        void allFiltersCombined() {
            PageRequest pr = new PageRequest(2, 5);
            pr.setSearch("PJ");
            pr.setStatusFilter("LUNAS");
            pr.setStartDate("2024-01-01");
            pr.setEndDate("2024-12-31");
            when(repository.find(anyString(), any(Parameters.class))).thenReturn(panacheQuery);
            when(panacheQuery.count()).thenReturn(20L);
            when(panacheQuery.page(any())).thenReturn(panacheQuery);
            when(panacheQuery.list()).thenReturn(List.of(testEntity));

            PageResponse<TbPenjualanEntity> result = penjualanService.findPaginated(pr);

            assertEquals(20, result.getRowsNumber());
            assertEquals(1, result.getRows().size());
        }

        @Test
        @DisplayName("with empty search is ignored")
        void emptySearchIgnored() {
            PageRequest pr = new PageRequest(1, 10);
            pr.setSearch("");
            when(repository.find(anyString(), any(Parameters.class))).thenReturn(panacheQuery);
            when(panacheQuery.count()).thenReturn(1L);
            when(panacheQuery.page(any())).thenReturn(panacheQuery);
            when(panacheQuery.list()).thenReturn(List.of(testEntity));

            PageResponse<TbPenjualanEntity> result = penjualanService.findPaginated(pr);

            assertEquals(1, result.getRowsNumber());
        }

        @Test
        @DisplayName("with null statusFilter is ignored")
        void nullStatusFilterIgnored() {
            PageRequest pr = new PageRequest(1, 10);
            pr.setStatusFilter(null);
            when(repository.find(anyString(), any(Parameters.class))).thenReturn(panacheQuery);
            when(panacheQuery.count()).thenReturn(1L);
            when(panacheQuery.page(any())).thenReturn(panacheQuery);
            when(panacheQuery.list()).thenReturn(List.of(testEntity));

            PageResponse<TbPenjualanEntity> result = penjualanService.findPaginated(pr);

            assertEquals(1, result.getRowsNumber());
        }

        @Test
        @DisplayName("with invalid startDate is gracefully ignored")
        void invalidStartDateIgnored() {
            PageRequest pr = new PageRequest(1, 10);
            pr.setStartDate("not-a-date");
            when(repository.find(anyString(), any(Parameters.class))).thenReturn(panacheQuery);
            when(panacheQuery.count()).thenReturn(1L);
            when(panacheQuery.page(any())).thenReturn(panacheQuery);
            when(panacheQuery.list()).thenReturn(List.of(testEntity));

            PageResponse<TbPenjualanEntity> result = penjualanService.findPaginated(pr);

            assertEquals(1, result.getRowsNumber());
        }

        @Test
        @DisplayName("with invalid endDate is gracefully ignored")
        void invalidEndDateIgnored() {
            PageRequest pr = new PageRequest(1, 10);
            pr.setEndDate("bad-date");
            when(repository.find(anyString(), any(Parameters.class))).thenReturn(panacheQuery);
            when(panacheQuery.count()).thenReturn(1L);
            when(panacheQuery.page(any())).thenReturn(panacheQuery);
            when(panacheQuery.list()).thenReturn(List.of(testEntity));

            PageResponse<TbPenjualanEntity> result = penjualanService.findPaginated(pr);

            assertEquals(1, result.getRowsNumber());
        }
    }

    // ─── cancelPenjualanBySpk ─────────────────────────────────────────

    @Nested
    @DisplayName("cancelPenjualanBySpk")
    class CancelPenjualanBySpk {

        @Test
        @DisplayName("deletes penjualan and resets SPK status to OPEN")
        void success() {
            PanacheQuery<TbPenjualanEntity> findQuery = mock(PanacheQuery.class);
            when(repository.find(eq("noSpk"), any(Object[].class))).thenReturn(findQuery);
            when(findQuery.firstResult()).thenReturn(testEntity);
            when(penjualanDetailRepository.delete(eq("noPenjualan"), any(Object[].class))).thenReturn(1L);

            TbSpkEntity spkEntity = new TbSpkEntity();
            spkEntity.setNoSpk("SPK001");
            spkEntity.setStatusSpk("MENUNGGU");
            spkEntity.setFinishedAt(java.time.LocalDateTime.now());

            PanacheQuery<TbSpkEntity> spkFindQuery = mock(PanacheQuery.class);
            when(spkRepository.find(eq("noSpk"), any(Object[].class))).thenReturn(spkFindQuery);
            when(spkFindQuery.firstResult()).thenReturn(spkEntity);

            EntityManager em = mock(EntityManager.class);
            when(spkRepository.getEntityManager()).thenReturn(em);
            when(em.merge(any(TbSpkEntity.class))).thenReturn(spkEntity);

            penjualanService.cancelPenjualanBySpk("SPK001");

            verify(penjualanDetailRepository).delete(eq("noPenjualan"), any(Object[].class));
            verify(repository).delete(testEntity);
            assertEquals("OPEN", spkEntity.getStatusSpk());
            assertNull(spkEntity.getFinishedAt());
            verify(em).merge(spkEntity);
        }

        @Test
        @DisplayName("throws when penjualan not found")
        void notFound() {
            PanacheQuery<TbPenjualanEntity> findQuery = mock(PanacheQuery.class);
            when(repository.find(eq("noSpk"), any(Object[].class))).thenReturn(findQuery);
            when(findQuery.firstResult()).thenReturn(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> penjualanService.cancelPenjualanBySpk("SPK_NONE"));
            assertTrue(ex.getMessage().contains("not found"));
        }

        @Test
        @DisplayName("deletes penjualan even when SPK not found")
        void spkNotFoundStillDeletesPenjualan() {
            PanacheQuery<TbPenjualanEntity> findQuery = mock(PanacheQuery.class);
            when(repository.find(eq("noSpk"), any(Object[].class))).thenReturn(findQuery);
            when(findQuery.firstResult()).thenReturn(testEntity);
            when(penjualanDetailRepository.delete(eq("noPenjualan"), any(Object[].class))).thenReturn(0L);

            PanacheQuery<TbSpkEntity> spkFindQuery = mock(PanacheQuery.class);
            when(spkRepository.find(eq("noSpk"), any(Object[].class))).thenReturn(spkFindQuery);
            when(spkFindQuery.firstResult()).thenReturn(null);

            penjualanService.cancelPenjualanBySpk("SPK001");

            verify(repository).delete(testEntity);
            verify(penjualanDetailRepository).delete(eq("noPenjualan"), any(Object[].class));
        }
    }
}
