package com.github.b3kt.application.service.customer;

import com.github.b3kt.application.dto.customer.CustomerSpkDto;
import com.github.b3kt.application.dto.pazaauto.PenjualanPrintDto;
import com.github.b3kt.application.service.pazaauto.TbPenjualanService;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPenjualanEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSpkEntity;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbSpkRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CustomerSpkQueryService Tests")
class CustomerSpkQueryServiceTest {

    @Mock
    TbSpkRepository spkRepository;

    @Mock
    TbPenjualanService penjualanService;

    @Mock
    PanacheQuery<TbSpkEntity> query;

    @InjectMocks
    CustomerSpkQueryService service;

    private TbSpkEntity spk(Long id, Long pelangganId) {
        TbSpkEntity spk = new TbSpkEntity();
        spk.setId(id);
        spk.setPelangganId(pelangganId);
        spk.setNoSpk("SPK000" + id);
        spk.setTanggalJamSpk("2026-01-02 10:00:00");
        spk.setNopol("B1234XYZ");
        spk.setStatusSpk("PROSES");
        spk.setKeterangan("ganti oli");
        return spk;
    }

    @Nested
    @DisplayName("listForPelanggan")
    class ListForPelangganTests {

        @Test
        @DisplayName("maps every work order of the pelanggan, without pricing fields")
        void maps() {
            when(spkRepository.find(anyString(), any(Object[].class))).thenReturn(query);
            when(query.list()).thenReturn(List.of(spk(1L, 7L), spk(2L, 7L)));

            List<CustomerSpkDto> dtos = service.listForPelanggan(7L);

            assertEquals(2, dtos.size());
            CustomerSpkDto dto = dtos.get(0);
            assertEquals(1L, dto.getId());
            assertEquals("SPK0001", dto.getNoSpk());
            assertEquals("2026-01-02 10:00:00", dto.getTanggalJamSpk());
            assertEquals("B1234XYZ", dto.getNopol());
            assertEquals("PROSES", dto.getStatusSpk());
            assertEquals("ganti oli", dto.getKeterangan());
        }

        @Test
        @DisplayName("returns an empty list when the pelanggan has no work orders")
        void empty() {
            when(spkRepository.find(anyString(), any(Object[].class))).thenReturn(query);
            when(query.list()).thenReturn(List.of());

            assertTrue(service.listForPelanggan(7L).isEmpty());
        }
    }

    @Nested
    @DisplayName("getForPelanggan")
    class GetForPelangganTests {

        @Test
        @DisplayName("returns the work order when it belongs to the caller")
        void owned() {
            when(spkRepository.findByIdOptional(1L)).thenReturn(Optional.of(spk(1L, 7L)));

            CustomerSpkDto dto = service.getForPelanggan(1L, 7L);

            assertNotNull(dto);
            assertEquals(1L, dto.getId());
        }

        @Test
        @DisplayName("returns null when the work order belongs to a different pelanggan")
        void otherPelanggan() {
            when(spkRepository.findByIdOptional(1L)).thenReturn(Optional.of(spk(1L, 8L)));

            assertNull(service.getForPelanggan(1L, 7L));
        }

        @Test
        @DisplayName("returns null when the work order does not exist")
        void missing() {
            when(spkRepository.findByIdOptional(1L)).thenReturn(Optional.empty());

            assertNull(service.getForPelanggan(1L, 7L));
        }
    }

    @Nested
    @DisplayName("getInvoiceForPelanggan")
    class GetInvoiceForPelangganTests {

        private TbPenjualanEntity penjualan(Long pelangganId) {
            TbPenjualanEntity penjualan = new TbPenjualanEntity();
            penjualan.setNoPenjualan("PJ0001");
            penjualan.setNoSpk("SPK0001");
            penjualan.setPelangganId(pelangganId);
            return penjualan;
        }

        @Test
        @DisplayName("returns the faktur when the work order and penjualan belong to the caller")
        void owned() {
            PenjualanPrintDto expected = new PenjualanPrintDto();
            when(spkRepository.findByIdOptional(1L)).thenReturn(Optional.of(spk(1L, 7L)));
            when(penjualanService.findByNoSpk("SPK0001")).thenReturn(penjualan(7L));
            when(penjualanService.buildPrintDto("PJ0001")).thenReturn(expected);

            assertSame(expected, service.getInvoiceForPelanggan(1L, 7L));
        }

        @Test
        @DisplayName("returns the faktur when the penjualan has no pelanggan id (falls back to the SPK)")
        void penjualanWithoutPelanggan() {
            PenjualanPrintDto expected = new PenjualanPrintDto();
            when(spkRepository.findByIdOptional(1L)).thenReturn(Optional.of(spk(1L, 7L)));
            when(penjualanService.findByNoSpk("SPK0001")).thenReturn(penjualan(null));
            when(penjualanService.buildPrintDto("PJ0001")).thenReturn(expected);

            assertSame(expected, service.getInvoiceForPelanggan(1L, 7L));
        }

        @Test
        @DisplayName("returns null and never looks up a penjualan for another pelanggan's work order")
        void otherPelangganSpk() {
            when(spkRepository.findByIdOptional(1L)).thenReturn(Optional.of(spk(1L, 8L)));

            assertNull(service.getInvoiceForPelanggan(1L, 7L));
            verifyNoInteractions(penjualanService);
        }

        @Test
        @DisplayName("returns null when the work order does not exist")
        void missingSpk() {
            when(spkRepository.findByIdOptional(1L)).thenReturn(Optional.empty());

            assertNull(service.getInvoiceForPelanggan(1L, 7L));
            verifyNoInteractions(penjualanService);
        }

        @Test
        @DisplayName("returns null when no penjualan has been raised yet")
        void noPenjualan() {
            when(spkRepository.findByIdOptional(1L)).thenReturn(Optional.of(spk(1L, 7L)));
            when(penjualanService.findByNoSpk("SPK0001")).thenReturn(null);

            assertNull(service.getInvoiceForPelanggan(1L, 7L));
        }

        @Test
        @DisplayName("returns null when the penjualan belongs to a different pelanggan (stale/relinked data)")
        void penjualanOtherPelanggan() {
            when(spkRepository.findByIdOptional(1L)).thenReturn(Optional.of(spk(1L, 7L)));
            when(penjualanService.findByNoSpk("SPK0001")).thenReturn(penjualan(9L));

            assertNull(service.getInvoiceForPelanggan(1L, 7L));
            verify(penjualanService, never()).buildPrintDto(anyString());
        }
    }
}
