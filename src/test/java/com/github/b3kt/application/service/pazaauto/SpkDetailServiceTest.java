package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.infrastructure.persistence.entity.pazaauto.*;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbBarangRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbJasaRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbSpkDetailRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpkDetailServiceTest {

    @Mock
    TbSpkDetailRepository detailRepository;

    @Mock
    TbBarangRepository barangRepository;

    @Mock
    TbJasaRepository jasaRepository;

    @Mock
    PanacheQuery<TbSpkDetailEntity> query;

    @InjectMocks
    SpkDetailService service;

    private TbSpkEntity spk;

    @BeforeEach
    void setUp() {
        spk = new TbSpkEntity();
        spk.setNoSpk("SPK0105");
    }

    @Test
    @DisplayName("saveDetails with sparepart sets hargaMaster from barang")
    void testSaveDetails_withSparepart() {
        TbBarangEntity barang = new TbBarangEntity();
        barang.setId(10L);
        barang.setHargaJual(new BigDecimal("75000"));

        TbSpkDetailEntity detail = new TbSpkDetailEntity();
        detail.setSparepartId(10L);
        detail.setJumlah(2);

        spk.setDetails(List.of(detail));
        when(barangRepository.findByIdOptional(10L)).thenReturn(Optional.of(barang));
        doNothing().when(detailRepository).persist(any(TbSpkDetailEntity.class));

        service.saveDetails(spk);

        assertEquals(new BigDecimal("75000"), detail.getHargaMaster());
        assertEquals(new BigDecimal("75000"), detail.getHarga());
        assertEquals("SPK0105", detail.getId().getNoSpk());
        verify(detailRepository).persist(detail);
    }

    @Test
    @DisplayName("saveDetails with jasa sets hargaMaster from jasa")
    void testSaveDetails_withJasa() {
        TbJasaEntity jasa = new TbJasaEntity();
        jasa.setId(20L);
        jasa.setHargaJasa(100000);

        TbSpkDetailEntity detail = new TbSpkDetailEntity();
        detail.setJasaId(20L);
        detail.setJumlah(1);

        spk.setDetails(List.of(detail));
        when(jasaRepository.findByIdOptional(20L)).thenReturn(Optional.of(jasa));
        doNothing().when(detailRepository).persist(any(TbSpkDetailEntity.class));

        service.saveDetails(spk);

        assertEquals(new BigDecimal("100000"), detail.getHargaMaster());
        assertEquals(new BigDecimal("100000"), detail.getHarga());
        verify(detailRepository).persist(detail);
    }

    @Test
    @DisplayName("saveDetails does not override existing harga")
    void testSaveDetails_hargaAlreadySet() {
        TbBarangEntity barang = new TbBarangEntity();
        barang.setId(10L);
        barang.setHargaJual(new BigDecimal("75000"));

        TbSpkDetailEntity detail = new TbSpkDetailEntity();
        detail.setSparepartId(10L);
        detail.setHarga(new BigDecimal("80000"));
        detail.setJumlah(1);

        spk.setDetails(List.of(detail));
        when(barangRepository.findByIdOptional(10L)).thenReturn(Optional.of(barang));
        doNothing().when(detailRepository).persist(any(TbSpkDetailEntity.class));

        service.saveDetails(spk);

        assertEquals(new BigDecimal("75000"), detail.getHargaMaster());
        assertEquals(new BigDecimal("80000"), detail.getHarga());
        verify(detailRepository).persist(detail);
    }

    @Test
    @DisplayName("saveDetails with null details does nothing")
    void testSaveDetails_nullDetails() {
        spk.setDetails(null);

        service.saveDetails(spk);

        verifyNoInteractions(detailRepository, barangRepository, jasaRepository);
    }

    @Test
    @DisplayName("deleteDetailsByNoSpk delegates to repository")
    void testDeleteDetailsByNoSpk() {
        when(detailRepository.delete(eq("id.noSpk"), eq("SPK0105"))).thenReturn(1L);

        service.deleteDetailsByNoSpk("SPK0105");

        verify(detailRepository).delete("id.noSpk", "SPK0105");
    }

    @Test
    @DisplayName("findByNoSpk returns matching details")
    void testFindByNoSpk() {
        TbSpkDetailEntity detail = new TbSpkDetailEntity();
        detail.setId(new TbSpkDetailId("SPK0105", "Service Rutin"));

        when(detailRepository.find(eq("id.noSpk"), eq("SPK0105"))).thenReturn(query);
        when(query.list()).thenReturn(List.of(detail));

        List<TbSpkDetailEntity> result = service.findByNoSpk("SPK0105");

        assertEquals(1, result.size());
        assertEquals("SPK0105", result.get(0).getId().getNoSpk());
    }
}
