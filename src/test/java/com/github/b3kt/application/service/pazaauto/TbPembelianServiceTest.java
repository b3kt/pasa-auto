package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPembelianDetailEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPembelianEntity;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbPembelianRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Parameters;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TbPembelianService Tests")
class TbPembelianServiceTest {

    @Mock TbPembelianRepository repository;
    @Mock TbPembelianDetailService detailService;
    @Mock TbSparepartService sparepartService;
    @Mock PanacheQuery<TbPembelianEntity> panacheQuery;
    @Mock EntityManager entityManager;

    @InjectMocks TbPembelianService service;

    private TbPembelianEntity testEntity;

    @BeforeEach
    void setUp() {
        testEntity = new TbPembelianEntity();
        testEntity.setId(1L);
        testEntity.setNoPembelian("FB-20240101-1");
        testEntity.setNoUrut(1);
        testEntity.setJenisPembelian("BARANG");
        testEntity.setGrandTotal(BigDecimal.valueOf(200000));
        testEntity.setTanggalPembelian(LocalDateTime.now());
        testEntity.setSupplierId(10);
        testEntity.setKeterangan("Test pembelian");
    }

    // ─── create ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("sets noUrut from noPembelian and persists")
        void setsNoUrutAndPersists() {
            TbPembelianEntity entity = new TbPembelianEntity();
            entity.setNoPembelian("FB-20240101-5");

            service.create(entity);

            assertEquals(5, entity.getNoUrut());
            verify(repository).persist(entity);
        }

        @Test
        @DisplayName("noPembelian with fewer than 3 parts leaves noUrut null")
        void invalidFormatLeavesNoUrutNull() {
            TbPembelianEntity entity = new TbPembelianEntity();
            entity.setNoPembelian("FB-20240101");

            service.create(entity);

            assertNull(entity.getNoUrut());
            verify(repository).persist(entity);
        }

        @Test
        @DisplayName("null noPembelian leaves noUrut null")
        void nullNoPembelianLeavesNoUrutNull() {
            TbPembelianEntity entity = new TbPembelianEntity();
            entity.setNoPembelian(null);

            service.create(entity);

            assertNull(entity.getNoUrut());
            verify(repository).persist(entity);
        }

        @Test
        @DisplayName("empty noPembelian leaves noUrut null")
        void emptyNoPembelianLeavesNoUrutNull() {
            TbPembelianEntity entity = new TbPembelianEntity();
            entity.setNoPembelian("");

            service.create(entity);

            assertNull(entity.getNoUrut());
            verify(repository).persist(entity);
        }

        @Test
        @DisplayName("noPembelian with non-numeric third part leaves noUrut null")
        void nonNumericNoUrutIgnored() {
            TbPembelianEntity entity = new TbPembelianEntity();
            entity.setNoPembelian("FB-20240101-abc");

            service.create(entity);

            assertNull(entity.getNoUrut());
            verify(repository).persist(entity);
        }
    }

    // ─── createWithDetails ────────────────────────────────────────────

    @Nested
    @DisplayName("createWithDetails")
    class CreateWithDetails {

        @Test
        @DisplayName("saves entity and details without stock integration")
        void savesWithoutStockIntegration() {
            service.stockIntegrationEnabled = false;
            TbPembelianEntity entity = new TbPembelianEntity();
            entity.setId(1L);
            entity.setNoPembelian("FB-20240101-2");
            entity.setJenisPembelian("SPAREPART");

            TbPembelianDetailEntity detail = new TbPembelianDetailEntity();
            detail.setSparepartId(10L);
            detail.setKuantiti(5);
            detail.setHarga(BigDecimal.valueOf(10000));
            detail.setTotal(BigDecimal.valueOf(50000));

            doNothing().when(repository).persist(any(TbPembelianEntity.class));

            TbPembelianEntity result = service.createWithDetails(entity, List.of(detail));

            assertNotNull(result);
            assertEquals(1L, detail.getPembelianId());
            verify(repository).persist(entity);
            verify(detailService).create(detail);
            verifyNoInteractions(sparepartService);
        }

        @Test
        @DisplayName("increases stock when SPAREPART and stockIntegrationEnabled")
        void increasesStockForSparepart() {
            service.stockIntegrationEnabled = true;
            TbPembelianEntity entity = new TbPembelianEntity();
            entity.setId(2L);
            entity.setNoPembelian("FS-20240101-1");
            entity.setJenisPembelian("SPAREPART");

            TbPembelianDetailEntity detail = new TbPembelianDetailEntity();
            detail.setSparepartId(10L);
            detail.setKuantiti(3);
            detail.setHarga(BigDecimal.valueOf(50000));
            detail.setTotal(BigDecimal.valueOf(150000));

            doNothing().when(repository).persist(any(TbPembelianEntity.class));

            TbPembelianEntity result = service.createWithDetails(entity, List.of(detail));

            assertNotNull(result);
            assertEquals(2L, detail.getPembelianId());
            verify(sparepartService).increaseStock(10L, 3);
        }

        @Test
        @DisplayName("does not increase stock when BARANG type even if enabled")
        void noStockIncreaseForBarang() {
            service.stockIntegrationEnabled = true;
            TbPembelianEntity entity = new TbPembelianEntity();
            entity.setNoPembelian("FB-20240101-1");
            entity.setJenisPembelian("BARANG");

            TbPembelianDetailEntity detail = new TbPembelianDetailEntity();
            detail.setSparepartId(10L);
            detail.setKuantiti(2);

            TbPembelianEntity saved = new TbPembelianEntity();
            saved.setId(3L);
            doNothing().when(repository).persist(any(TbPembelianEntity.class));

            service.createWithDetails(entity, List.of(detail));

            verifyNoInteractions(sparepartService);
        }

        @Test
        @DisplayName("skips stock increase when sparepartId is null")
        void skipsStockWhenSparepartIdNull() {
            service.stockIntegrationEnabled = true;
            TbPembelianEntity entity = new TbPembelianEntity();
            entity.setNoPembelian("FS-20240101-1");
            entity.setJenisPembelian("SPAREPART");

            TbPembelianDetailEntity detail = new TbPembelianDetailEntity();
            detail.setSparepartId(null);
            detail.setKuantiti(5);

            TbPembelianEntity saved = new TbPembelianEntity();
            saved.setId(4L);
            doNothing().when(repository).persist(any(TbPembelianEntity.class));

            service.createWithDetails(entity, List.of(detail));

            verifyNoInteractions(sparepartService);
        }

        @Test
        @DisplayName("skips stock increase when kuantiti is null")
        void skipsStockWhenKuantitiNull() {
            service.stockIntegrationEnabled = true;
            TbPembelianEntity entity = new TbPembelianEntity();
            entity.setNoPembelian("FS-20240101-1");
            entity.setJenisPembelian("SPAREPART");

            TbPembelianDetailEntity detail = new TbPembelianDetailEntity();
            detail.setSparepartId(10L);
            detail.setKuantiti(null);

            TbPembelianEntity saved = new TbPembelianEntity();
            saved.setId(5L);
            doNothing().when(repository).persist(any(TbPembelianEntity.class));

            service.createWithDetails(entity, List.of(detail));

            verifyNoInteractions(sparepartService);
        }

        @Test
        @DisplayName("handles null details list")
        void handlesNullDetails() {
            TbPembelianEntity entity = new TbPembelianEntity();
            entity.setNoPembelian("FB-20240101-1");
            entity.setJenisPembelian("BARANG");

            TbPembelianEntity saved = new TbPembelianEntity();
            saved.setId(6L);
            doNothing().when(repository).persist(any(TbPembelianEntity.class));

            TbPembelianEntity result = service.createWithDetails(entity, null);

            assertNotNull(result);
            verify(detailService, never()).create(any());
        }

        @Test
        @DisplayName("handles empty details list")
        void handlesEmptyDetails() {
            TbPembelianEntity entity = new TbPembelianEntity();
            entity.setNoPembelian("FB-20240101-1");
            entity.setJenisPembelian("BARANG");

            TbPembelianEntity saved = new TbPembelianEntity();
            saved.setId(7L);
            doNothing().when(repository).persist(any(TbPembelianEntity.class));

            TbPembelianEntity result = service.createWithDetails(entity, List.of());

            assertNotNull(result);
            verify(detailService, never()).create(any());
        }

        @Test
        @DisplayName("saves multiple details with stock increase for each")
        void multipleDetailsWithStock() {
            service.stockIntegrationEnabled = true;
            TbPembelianEntity entity = new TbPembelianEntity();
            entity.setNoPembelian("FS-20240101-1");
            entity.setJenisPembelian("SPAREPART");

            TbPembelianDetailEntity d1 = new TbPembelianDetailEntity();
            d1.setSparepartId(10L);
            d1.setKuantiti(2);

            TbPembelianDetailEntity d2 = new TbPembelianDetailEntity();
            d2.setSparepartId(20L);
            d2.setKuantiti(3);

            TbPembelianEntity saved = new TbPembelianEntity();
            saved.setId(8L);
            doNothing().when(repository).persist(any(TbPembelianEntity.class));

            service.createWithDetails(entity, List.of(d1, d2));

            verify(sparepartService).increaseStock(10L, 2);
            verify(sparepartService).increaseStock(20L, 3);
            verify(detailService, times(2)).create(any());
        }
    }

    // ─── update ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("reverses old stock then updates when SPAREPART + enabled")
        void reversesStockForSparepart() {
            service.stockIntegrationEnabled = true;

            TbPembelianEntity spEntity = new TbPembelianEntity();
            spEntity.setId(1L);
            spEntity.setJenisPembelian("SPAREPART");

            TbPembelianDetailEntity oldDetail = new TbPembelianDetailEntity();
            oldDetail.setSparepartId(10L);
            oldDetail.setKuantiti(5);

            when(repository.findByIdOptional(1L)).thenReturn(Optional.of(spEntity));
            when(detailService.findByPembelianId(1L)).thenReturn(List.of(oldDetail));

            TbPembelianEntity updateEntity = new TbPembelianEntity();
            updateEntity.setNoPembelian("FB-20240101-1");
            updateEntity.setNoUrut(1);

            when(repository.getEntityManager()).thenReturn(entityManager);
            when(entityManager.merge(any(TbPembelianEntity.class))).thenReturn(updateEntity);

            service.update(1L, updateEntity);

            verify(sparepartService).decreaseStock(10L, 5);
            verify(entityManager).merge(any(TbPembelianEntity.class));
        }

        @Test
        @DisplayName("does not reverse stock when BARANG type even if enabled")
        void noStockReversalForBarang() {
            service.stockIntegrationEnabled = true;

            when(repository.findByIdOptional(1L)).thenReturn(Optional.of(testEntity));
            when(detailService.findByPembelianId(1L)).thenReturn(List.of());

            TbPembelianEntity updateEntity = new TbPembelianEntity();
            updateEntity.setNoPembelian("FB-20240101-1");
            updateEntity.setNoUrut(1);

            when(repository.getEntityManager()).thenReturn(entityManager);
            when(entityManager.merge(any(TbPembelianEntity.class))).thenReturn(updateEntity);

            service.update(1L, updateEntity);

            verifyNoInteractions(sparepartService);
        }

        @Test
        @DisplayName("does not reverse stock when disabled")
        void noStockReversalWhenDisabled() {
            service.stockIntegrationEnabled = false;

            when(repository.findByIdOptional(1L)).thenReturn(Optional.of(testEntity));
            when(detailService.findByPembelianId(1L)).thenReturn(List.of());

            TbPembelianEntity updateEntity = new TbPembelianEntity();
            updateEntity.setNoPembelian("FB-20240101-1");
            updateEntity.setNoUrut(1);

            when(repository.getEntityManager()).thenReturn(entityManager);
            when(entityManager.merge(any(TbPembelianEntity.class))).thenReturn(updateEntity);

            service.update(1L, updateEntity);

            verifyNoInteractions(sparepartService);
        }

        @Test
        @DisplayName("reverses stock for multiple old details")
        void reversesStockForMultipleDetails() {
            service.stockIntegrationEnabled = true;

            TbPembelianEntity spEntity = new TbPembelianEntity();
            spEntity.setId(1L);
            spEntity.setJenisPembelian("SPAREPART");

            TbPembelianDetailEntity od1 = new TbPembelianDetailEntity();
            od1.setSparepartId(10L);
            od1.setKuantiti(3);

            TbPembelianDetailEntity od2 = new TbPembelianDetailEntity();
            od2.setSparepartId(20L);
            od2.setKuantiti(7);

            when(repository.findByIdOptional(1L)).thenReturn(Optional.of(spEntity));
            when(detailService.findByPembelianId(1L)).thenReturn(List.of(od1, od2));

            TbPembelianEntity updateEntity = new TbPembelianEntity();
            updateEntity.setNoPembelian("FB-20240101-1");
            updateEntity.setNoUrut(1);

            when(repository.getEntityManager()).thenReturn(entityManager);
            when(entityManager.merge(any(TbPembelianEntity.class))).thenReturn(updateEntity);

            service.update(1L, updateEntity);

            verify(sparepartService).decreaseStock(10L, 3);
            verify(sparepartService).decreaseStock(20L, 7);
        }

        @Test
        @DisplayName("skips stock reversal for detail with null sparepartId")
        void skipsReversalForNullSparepartId() {
            service.stockIntegrationEnabled = true;

            TbPembelianDetailEntity od = new TbPembelianDetailEntity();
            od.setSparepartId(null);
            od.setKuantiti(5);

            when(repository.findByIdOptional(1L)).thenReturn(Optional.of(testEntity));
            when(detailService.findByPembelianId(1L)).thenReturn(List.of(od));

            TbPembelianEntity updateEntity = new TbPembelianEntity();
            updateEntity.setNoPembelian("FB-20240101-1");
            updateEntity.setNoUrut(1);

            when(repository.getEntityManager()).thenReturn(entityManager);
            when(entityManager.merge(any(TbPembelianEntity.class))).thenReturn(updateEntity);

            service.update(1L, updateEntity);

            verifyNoInteractions(sparepartService);
        }

        @Test
        @DisplayName("skips stock reversal for detail with null kuantiti")
        void skipsReversalForNullKuantiti() {
            service.stockIntegrationEnabled = true;

            TbPembelianDetailEntity od = new TbPembelianDetailEntity();
            od.setSparepartId(10L);
            od.setKuantiti(null);

            when(repository.findByIdOptional(1L)).thenReturn(Optional.of(testEntity));
            when(detailService.findByPembelianId(1L)).thenReturn(List.of(od));

            TbPembelianEntity updateEntity = new TbPembelianEntity();
            updateEntity.setNoPembelian("FB-20240101-1");
            updateEntity.setNoUrut(1);

            when(repository.getEntityManager()).thenReturn(entityManager);
            when(entityManager.merge(any(TbPembelianEntity.class))).thenReturn(updateEntity);

            service.update(1L, updateEntity);

            verifyNoInteractions(sparepartService);
        }

        @Test
        @DisplayName("updates item details with existing id calls detailService.update")
        void updatesExistingDetail() {
            service.stockIntegrationEnabled = false;

            TbPembelianDetailEntity detail = new TbPembelianDetailEntity();
            detail.setId(100L);
            detail.setTotal(BigDecimal.valueOf(250000));

            TbPembelianEntity updateEntity = new TbPembelianEntity();
            updateEntity.setNoPembelian("FB-20240101-1");
            updateEntity.setNoUrut(1);
            updateEntity.setDetails(List.of(detail));

            when(repository.findByIdOptional(1L)).thenReturn(Optional.of(testEntity));
            when(detailService.findByPembelianId(1L)).thenReturn(List.of());
            when(repository.getEntityManager()).thenReturn(entityManager);
            when(entityManager.merge(any(TbPembelianEntity.class))).thenReturn(updateEntity);

            service.update(1L, updateEntity);

            verify(detailService).update(100L, detail);
            assertEquals(BigDecimal.valueOf(250000), updateEntity.getGrandTotal());
        }

        @Test
        @DisplayName("updates item details with null id calls detailService.create")
        void createsNewDetailOnUpdate() {
            service.stockIntegrationEnabled = false;

            TbPembelianDetailEntity detail = new TbPembelianDetailEntity();
            detail.setId(null);
            detail.setTotal(BigDecimal.valueOf(100000));

            TbPembelianEntity updateEntity = new TbPembelianEntity();
            updateEntity.setNoPembelian("FB-20240101-1");
            updateEntity.setNoUrut(1);
            updateEntity.setDetails(List.of(detail));

            when(repository.findByIdOptional(1L)).thenReturn(Optional.of(testEntity));
            when(detailService.findByPembelianId(1L)).thenReturn(List.of());
            when(repository.getEntityManager()).thenReturn(entityManager);
            when(entityManager.merge(any(TbPembelianEntity.class))).thenReturn(updateEntity);

            service.update(1L, updateEntity);

            verify(detailService).create(detail);
            assertEquals(BigDecimal.valueOf(100000), updateEntity.getGrandTotal());
        }

        @Test
        @DisplayName("does not update details when entity details is null")
        void noDetailsUpdateWhenNull() {
            service.stockIntegrationEnabled = false;

            TbPembelianEntity updateEntity = new TbPembelianEntity();
            updateEntity.setNoPembelian("FB-20240101-1");
            updateEntity.setNoUrut(1);
            updateEntity.setDetails(null);

            when(repository.findByIdOptional(1L)).thenReturn(Optional.of(testEntity));
            when(detailService.findByPembelianId(1L)).thenReturn(List.of());
            when(repository.getEntityManager()).thenReturn(entityManager);
            when(entityManager.merge(any(TbPembelianEntity.class))).thenReturn(updateEntity);

            service.update(1L, updateEntity);

            verify(detailService, never()).update(anyLong(), any());
            verify(detailService, never()).create(any());
        }

        @Test
        @DisplayName("does not update details when entity details is empty")
        void noDetailsUpdateWhenEmpty() {
            service.stockIntegrationEnabled = false;

            TbPembelianEntity updateEntity = new TbPembelianEntity();
            updateEntity.setNoPembelian("FB-20240101-1");
            updateEntity.setNoUrut(1);
            updateEntity.setDetails(List.of());

            when(repository.findByIdOptional(1L)).thenReturn(Optional.of(testEntity));
            when(detailService.findByPembelianId(1L)).thenReturn(List.of());
            when(repository.getEntityManager()).thenReturn(entityManager);
            when(entityManager.merge(any(TbPembelianEntity.class))).thenReturn(updateEntity);

            service.update(1L, updateEntity);

            verify(detailService, never()).update(anyLong(), any());
            verify(detailService, never()).create(any());
        }

        @Test
        @DisplayName("aggregates grandTotal from multiple details")
        void aggregatesGrandTotal() {
            service.stockIntegrationEnabled = false;

            TbPembelianDetailEntity d1 = new TbPembelianDetailEntity();
            d1.setId(100L);
            d1.setTotal(BigDecimal.valueOf(100000));

            TbPembelianDetailEntity d2 = new TbPembelianDetailEntity();
            d2.setId(null);
            d2.setTotal(BigDecimal.valueOf(200000));

            TbPembelianEntity updateEntity = new TbPembelianEntity();
            updateEntity.setNoPembelian("FB-20240101-1");
            updateEntity.setNoUrut(1);
            updateEntity.setDetails(List.of(d1, d2));

            when(repository.findByIdOptional(1L)).thenReturn(Optional.of(testEntity));
            when(detailService.findByPembelianId(1L)).thenReturn(List.of());
            when(repository.getEntityManager()).thenReturn(entityManager);
            when(entityManager.merge(any(TbPembelianEntity.class))).thenReturn(updateEntity);

            service.update(1L, updateEntity);

            assertEquals(BigDecimal.valueOf(300000), updateEntity.getGrandTotal());
        }

        @Test
        @DisplayName("throws EntityNotFoundException when entity not found")
        void throwsWhenNotFound() {
            when(repository.findByIdOptional(999L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> service.update(999L, testEntity));
        }
    }

    // ─── updateWithDetails ────────────────────────────────────────────

    @Nested
    @DisplayName("updateWithDetails")
    class UpdateWithDetails {

        @Test
        @DisplayName("deletes old details and creates new ones with stock increase")
        void replacesDetails() {
            service.stockIntegrationEnabled = true;

            TbPembelianEntity updateEntity = new TbPembelianEntity();
            updateEntity.setNoPembelian("FS-20240101-1");
            updateEntity.setJenisPembelian("SPAREPART");

            TbPembelianDetailEntity newDetail = new TbPembelianDetailEntity();
            newDetail.setSparepartId(15L);
            newDetail.setKuantiti(4);
            newDetail.setTotal(BigDecimal.valueOf(200000));

            when(repository.findByIdOptional(1L)).thenReturn(Optional.of(testEntity));
            when(detailService.findByPembelianId(1L)).thenReturn(List.of());
            when(repository.getEntityManager()).thenReturn(entityManager);
            when(entityManager.merge(any(TbPembelianEntity.class))).thenReturn(updateEntity);

            TbPembelianEntity result = service.updateWithDetails(1L, updateEntity, List.of(newDetail));

            assertNotNull(result);
            verify(detailService).deleteByPembelianId(1L);
            assertEquals(1L, newDetail.getPembelianId());
            assertNull(newDetail.getId());
            verify(detailService).create(newDetail);
            verify(sparepartService).increaseStock(15L, 4);
        }

        @Test
        @DisplayName("handles null new details")
        void handlesNullNewDetails() {
            service.stockIntegrationEnabled = false;

            when(repository.findByIdOptional(1L)).thenReturn(Optional.of(testEntity));
            when(detailService.findByPembelianId(1L)).thenReturn(List.of());
            when(repository.getEntityManager()).thenReturn(entityManager);
            when(entityManager.merge(any(TbPembelianEntity.class))).thenReturn(testEntity);

            TbPembelianEntity result = service.updateWithDetails(1L, testEntity, null);

            assertNotNull(result);
            verify(detailService).deleteByPembelianId(1L);
            verify(detailService, never()).create(any());
        }

        @Test
        @DisplayName("handles empty new details")
        void handlesEmptyNewDetails() {
            service.stockIntegrationEnabled = false;

            when(repository.findByIdOptional(1L)).thenReturn(Optional.of(testEntity));
            when(detailService.findByPembelianId(1L)).thenReturn(List.of());
            when(repository.getEntityManager()).thenReturn(entityManager);
            when(entityManager.merge(any(TbPembelianEntity.class))).thenReturn(testEntity);

            TbPembelianEntity result = service.updateWithDetails(1L, testEntity, List.of());

            assertNotNull(result);
            verify(detailService).deleteByPembelianId(1L);
            verify(detailService, never()).create(any());
        }

        @Test
        @DisplayName("no stock increase when BARANG type")
        void noStockIncreaseForBarang() {
            service.stockIntegrationEnabled = true;

            TbPembelianEntity updateEntity = new TbPembelianEntity();
            updateEntity.setNoPembelian("FB-20240101-1");
            updateEntity.setJenisPembelian("BARANG");

            TbPembelianDetailEntity newDetail = new TbPembelianDetailEntity();
            newDetail.setSparepartId(15L);
            newDetail.setKuantiti(4);

            when(repository.findByIdOptional(1L)).thenReturn(Optional.of(testEntity));
            when(detailService.findByPembelianId(1L)).thenReturn(List.of());
            when(repository.getEntityManager()).thenReturn(entityManager);
            when(entityManager.merge(any(TbPembelianEntity.class))).thenReturn(updateEntity);

            service.updateWithDetails(1L, updateEntity, List.of(newDetail));

            verifyNoInteractions(sparepartService);
        }
    }

    // ─── delete ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("reverses stock, deletes details, and deletes entity when SPAREPART + enabled")
        void reversesStockAndDeletes() {
            service.stockIntegrationEnabled = true;

            TbPembelianEntity spEntity = new TbPembelianEntity();
            spEntity.setId(1L);
            spEntity.setJenisPembelian("SPAREPART");

            TbPembelianDetailEntity detail = new TbPembelianDetailEntity();
            detail.setSparepartId(10L);
            detail.setKuantiti(5);

            when(repository.findByIdOptional(1L)).thenReturn(Optional.of(spEntity));
            when(detailService.findByPembelianId(1L)).thenReturn(List.of(detail));
            when(repository.deleteById(1L)).thenReturn(true);

            service.delete(1L);

            verify(sparepartService).decreaseStock(10L, 5);
            verify(detailService).deleteByPembelianId(1L);
            verify(repository).deleteById(1L);
        }

        @Test
        @DisplayName("does not reverse stock when disabled")
        void noStockReversalWhenDisabled() {
            service.stockIntegrationEnabled = false;

            when(repository.findByIdOptional(1L)).thenReturn(Optional.of(testEntity));
            when(detailService.findByPembelianId(1L)).thenReturn(List.of());
            when(repository.deleteById(1L)).thenReturn(true);

            service.delete(1L);

            verifyNoInteractions(sparepartService);
            verify(detailService).deleteByPembelianId(1L);
            verify(repository).deleteById(1L);
        }

        @Test
        @DisplayName("does not reverse stock when BARANG type")
        void noStockReversalForBarang() {
            service.stockIntegrationEnabled = true;

            when(repository.findByIdOptional(1L)).thenReturn(Optional.of(testEntity));
            when(detailService.findByPembelianId(1L)).thenReturn(List.of());
            when(repository.deleteById(1L)).thenReturn(true);

            service.delete(1L);

            verifyNoInteractions(sparepartService);
        }

        @Test
        @DisplayName("reverses stock for multiple details")
        void reversesStockMultiple() {
            service.stockIntegrationEnabled = true;

            TbPembelianEntity spEntity = new TbPembelianEntity();
            spEntity.setId(1L);
            spEntity.setJenisPembelian("SPAREPART");

            TbPembelianDetailEntity d1 = new TbPembelianDetailEntity();
            d1.setSparepartId(10L);
            d1.setKuantiti(3);
            TbPembelianDetailEntity d2 = new TbPembelianDetailEntity();
            d2.setSparepartId(20L);
            d2.setKuantiti(7);

            when(repository.findByIdOptional(1L)).thenReturn(Optional.of(spEntity));
            when(detailService.findByPembelianId(1L)).thenReturn(List.of(d1, d2));
            when(repository.deleteById(1L)).thenReturn(true);

            service.delete(1L);

            verify(sparepartService).decreaseStock(10L, 3);
            verify(sparepartService).decreaseStock(20L, 7);
        }

        @Test
        @DisplayName("skips detail with null sparepartId during reversal")
        void skipsNullSparepartId() {
            service.stockIntegrationEnabled = true;

            TbPembelianEntity spEntity = new TbPembelianEntity();
            spEntity.setId(1L);
            spEntity.setJenisPembelian("SPAREPART");

            TbPembelianDetailEntity d1 = new TbPembelianDetailEntity();
            d1.setSparepartId(null);
            d1.setKuantiti(5);

            when(repository.findByIdOptional(1L)).thenReturn(Optional.of(spEntity));
            when(detailService.findByPembelianId(1L)).thenReturn(List.of(d1));
            when(repository.deleteById(1L)).thenReturn(true);

            service.delete(1L);

            verifyNoInteractions(sparepartService);
        }

        @Test
        @DisplayName("throws EntityNotFoundException when entity not found")
        void throwsWhenNotFound() {
            when(repository.findByIdOptional(999L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> service.delete(999L));
        }

        @Test
        @DisplayName("handles empty detail list during deletion")
        void handlesEmptyDetails() {
            service.stockIntegrationEnabled = true;

            when(repository.findByIdOptional(1L)).thenReturn(Optional.of(testEntity));
            when(detailService.findByPembelianId(1L)).thenReturn(List.of());
            when(repository.deleteById(1L)).thenReturn(true);

            service.delete(1L);

            verifyNoInteractions(sparepartService);
            verify(detailService).deleteByPembelianId(1L);
            verify(repository).deleteById(1L);
        }
    }

    // ─── findPaginated ────────────────────────────────────────────────

    @Nested
    @DisplayName("findPaginated")
    class FindPaginated {

        @Test
        @DisplayName("without filters")
        void noFilters() {
            PageRequest pr = new PageRequest(1, 10);
            when(repository.find(anyString(), any(Parameters.class))).thenReturn(panacheQuery);
            when(panacheQuery.count()).thenReturn(1L);
            when(panacheQuery.page(any())).thenReturn(panacheQuery);
            when(panacheQuery.list()).thenReturn(List.of(testEntity));

            PageResponse<TbPembelianEntity> result = service.findPaginated(pr);

            assertEquals(1, result.getRowsNumber());
            assertEquals(1, result.getRows().size());
        }

        @Test
        @DisplayName("with search filter")
        void withSearch() {
            PageRequest pr = new PageRequest(1, 10);
            pr.setSearch("FB");
            when(repository.find(anyString(), any(Parameters.class))).thenReturn(panacheQuery);
            when(panacheQuery.count()).thenReturn(2L);
            when(panacheQuery.page(any())).thenReturn(panacheQuery);
            when(panacheQuery.list()).thenReturn(List.of(testEntity));

            PageResponse<TbPembelianEntity> result = service.findPaginated(pr);

            assertEquals(2, result.getRowsNumber());
        }

        @Test
        @DisplayName("with jenisPembelian filter")
        void withJenisPembelianFilter() {
            PageRequest pr = new PageRequest(1, 10);
            pr.setJenisPembelianFilter("SPAREPART");
            when(repository.find(anyString(), any(Parameters.class))).thenReturn(panacheQuery);
            when(panacheQuery.count()).thenReturn(3L);
            when(panacheQuery.page(any())).thenReturn(panacheQuery);
            when(panacheQuery.list()).thenReturn(List.of(testEntity));

            PageResponse<TbPembelianEntity> result = service.findPaginated(pr);

            assertEquals(3, result.getRowsNumber());
        }

        @Test
        @DisplayName("with kategoriOperasional filter")
        void withKategoriOperasionalFilter() {
            PageRequest pr = new PageRequest(1, 10);
            pr.setKategoriOperasionalFilter("OPERASIONAL");
            when(repository.find(anyString(), any(Parameters.class))).thenReturn(panacheQuery);
            when(panacheQuery.count()).thenReturn(4L);
            when(panacheQuery.page(any())).thenReturn(panacheQuery);
            when(panacheQuery.list()).thenReturn(List.of(testEntity));

            PageResponse<TbPembelianEntity> result = service.findPaginated(pr);

            assertEquals(4, result.getRowsNumber());
        }

        @Test
        @DisplayName("with status filter (single status)")
        void withSingleStatus() {
            PageRequest pr = new PageRequest(1, 10);
            pr.setStatusFilter("LUNAS");
            when(repository.find(anyString(), any(Parameters.class))).thenReturn(panacheQuery);
            when(panacheQuery.count()).thenReturn(1L);
            when(panacheQuery.page(any())).thenReturn(panacheQuery);
            when(panacheQuery.list()).thenReturn(List.of(testEntity));

            PageResponse<TbPembelianEntity> result = service.findPaginated(pr);

            assertEquals(1, result.getRowsNumber());
        }

        @Test
        @DisplayName("with status filter (multiple statuses comma-separated)")
        void withMultipleStatuses() {
            PageRequest pr = new PageRequest(1, 10);
            pr.setStatusFilter("LUNAS,BELUM_LUNAS");
            when(repository.find(anyString(), any(Parameters.class))).thenReturn(panacheQuery);
            when(panacheQuery.count()).thenReturn(5L);
            when(panacheQuery.page(any())).thenReturn(panacheQuery);
            when(panacheQuery.list()).thenReturn(List.of(testEntity));

            PageResponse<TbPembelianEntity> result = service.findPaginated(pr);

            assertEquals(5, result.getRowsNumber());
        }

        @Test
        @DisplayName("with startDate filter")
        void withStartDate() {
            PageRequest pr = new PageRequest(1, 10);
            pr.setStartDate("2024-01-01");
            when(repository.find(anyString(), any(Parameters.class))).thenReturn(panacheQuery);
            when(panacheQuery.count()).thenReturn(10L);
            when(panacheQuery.page(any())).thenReturn(panacheQuery);
            when(panacheQuery.list()).thenReturn(List.of(testEntity));

            PageResponse<TbPembelianEntity> result = service.findPaginated(pr);

            assertEquals(10, result.getRowsNumber());
        }

        @Test
        @DisplayName("with endDate filter")
        void withEndDate() {
            PageRequest pr = new PageRequest(1, 10);
            pr.setEndDate("2024-12-31");
            when(repository.find(anyString(), any(Parameters.class))).thenReturn(panacheQuery);
            when(panacheQuery.count()).thenReturn(20L);
            when(panacheQuery.page(any())).thenReturn(panacheQuery);
            when(panacheQuery.list()).thenReturn(List.of(testEntity));

            PageResponse<TbPembelianEntity> result = service.findPaginated(pr);

            assertEquals(20, result.getRowsNumber());
        }

        @Test
        @DisplayName("with all filters combined")
        void allFiltersCombined() {
            PageRequest pr = new PageRequest(2, 5);
            pr.setSearch("FB");
            pr.setJenisPembelianFilter("BARANG");
            pr.setKategoriOperasionalFilter("OPERASIONAL");
            pr.setStatusFilter("LUNAS");
            pr.setStartDate("2024-01-01");
            pr.setEndDate("2024-12-31");
            when(repository.find(anyString(), any(Parameters.class))).thenReturn(panacheQuery);
            when(panacheQuery.count()).thenReturn(50L);
            when(panacheQuery.page(any())).thenReturn(panacheQuery);
            when(panacheQuery.list()).thenReturn(List.of(testEntity));

            PageResponse<TbPembelianEntity> result = service.findPaginated(pr);

            assertEquals(50, result.getRowsNumber());
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

            PageResponse<TbPembelianEntity> result = service.findPaginated(pr);

            assertEquals(1, result.getRowsNumber());
        }

        @Test
        @DisplayName("with null filters are ignored")
        void nullFiltersIgnored() {
            PageRequest pr = new PageRequest(1, 10);
            pr.setSearch(null);
            pr.setJenisPembelianFilter(null);
            pr.setKategoriOperasionalFilter(null);
            pr.setStatusFilter(null);
            pr.setStartDate(null);
            pr.setEndDate(null);
            when(repository.find(anyString(), any(Parameters.class))).thenReturn(panacheQuery);
            when(panacheQuery.count()).thenReturn(1L);
            when(panacheQuery.page(any())).thenReturn(panacheQuery);
            when(panacheQuery.list()).thenReturn(List.of(testEntity));

            PageResponse<TbPembelianEntity> result = service.findPaginated(pr);

            assertEquals(1, result.getRowsNumber());
        }
    }

    // ─── generateNoPembelian ──────────────────────────────────────────

    @Nested
    @DisplayName("generateNoPembelian")
    class GenerateNoPembelian {

        @Test
        @DisplayName("SPAREPART returns prefix FS")
        void sparepartReturnsFS() {
            when(repository.findMaxNoUrut(any(LocalDateTime.class), any(LocalDateTime.class), eq("SPAREPART")))
                    .thenReturn(null);
            PanacheQuery countQuery = mock(PanacheQuery.class);
            when(repository.find(eq("noPembelian"), any(Object[].class))).thenReturn(countQuery);
            when(countQuery.count()).thenReturn(0L);

            String result = service.generateNoPembelian("SPAREPART");

            assertTrue(result.startsWith("FS"));
        }

        @Test
        @DisplayName("BARANG returns prefix FB")
        void barangReturnsFB() {
            when(repository.findMaxNoUrut(any(LocalDateTime.class), any(LocalDateTime.class), eq("BARANG")))
                    .thenReturn(null);
            PanacheQuery countQuery = mock(PanacheQuery.class);
            when(repository.find(eq("noPembelian"), any(Object[].class))).thenReturn(countQuery);
            when(countQuery.count()).thenReturn(0L);

            String result = service.generateNoPembelian("BARANG");

            assertTrue(result.startsWith("FB"));
        }

        @Test
        @DisplayName("OPERASIONAL returns prefix FO")
        void operasionalReturnsFO() {
            when(repository.findMaxNoUrut(any(LocalDateTime.class), any(LocalDateTime.class), eq("OPERASIONAL")))
                    .thenReturn(null);
            PanacheQuery countQuery = mock(PanacheQuery.class);
            when(repository.find(eq("noPembelian"), any(Object[].class))).thenReturn(countQuery);
            when(countQuery.count()).thenReturn(0L);

            String result = service.generateNoPembelian("OPERASIONAL");

            assertTrue(result.startsWith("FO"));
        }

        @Test
        @DisplayName("other type returns prefix FO")
        void otherTypeReturnsFO() {
            when(repository.findMaxNoUrut(any(LocalDateTime.class), any(LocalDateTime.class), eq("OTHER")))
                    .thenReturn(null);
            PanacheQuery countQuery = mock(PanacheQuery.class);
            when(repository.find(eq("noPembelian"), any(Object[].class))).thenReturn(countQuery);
            when(countQuery.count()).thenReturn(0L);

            String result = service.generateNoPembelian("OTHER");

            assertTrue(result.startsWith("FO"));
        }

        @Test
        @DisplayName("increments noUrut from maxNoUrut")
        void incrementsFromMax() {
            when(repository.findMaxNoUrut(any(LocalDateTime.class), any(LocalDateTime.class), eq("BARANG")))
                    .thenReturn(3);
            PanacheQuery countQuery = mock(PanacheQuery.class);
            when(repository.find(eq("noPembelian"), any(Object[].class))).thenReturn(countQuery);
            when(countQuery.count()).thenReturn(0L);

            String result = service.generateNoPembelian("BARANG");

            assertTrue(result.startsWith("FB"));
            assertTrue(result.endsWith("4"));
        }

        @Test
        @DisplayName("collision-safe: increments when noPembelian already exists")
        void collisionSafe() {
            when(repository.findMaxNoUrut(any(LocalDateTime.class), any(LocalDateTime.class), eq("BARANG")))
                    .thenReturn(null);

            PanacheQuery countQuery1 = mock(PanacheQuery.class);
            PanacheQuery countQuery2 = mock(PanacheQuery.class);
            PanacheQuery countQuery3 = mock(PanacheQuery.class);

            when(repository.find(eq("noPembelian"), any(Object[].class)))
                    .thenReturn(countQuery1)
                    .thenReturn(countQuery2)
                    .thenReturn(countQuery3);
            when(countQuery1.count()).thenReturn(1L);
            when(countQuery2.count()).thenReturn(1L);
            when(countQuery3.count()).thenReturn(0L);

            String result = service.generateNoPembelian("BARANG");

            assertTrue(result.startsWith("FB"));
            // First noUrut was 1 (collision), second 2 (collision), third 3 (ok)
            assertTrue(result.endsWith("3"));
        }

        @Test
        @DisplayName("format is prefix + date + number")
        void formatCorrect() {
            when(repository.findMaxNoUrut(any(LocalDateTime.class), any(LocalDateTime.class), eq("SPAREPART")))
                    .thenReturn(null);
            PanacheQuery countQuery = mock(PanacheQuery.class);
            when(repository.find(eq("noPembelian"), any(Object[].class))).thenReturn(countQuery);
            when(countQuery.count()).thenReturn(0L);

            String result = service.generateNoPembelian("SPAREPART");

            // Should be FS + yyyyMMdd + 1
            String datePart = java.time.LocalDate.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
            assertEquals("FS" + datePart + "1", result);
        }
    }

    // ─── findByNoPembelian ────────────────────────────────────────────

    @Nested
    @DisplayName("findByNoPembelian")
    class FindByNoPembelian {

        @Test
        @DisplayName("returns entity when found")
        void found() {
            PanacheQuery<TbPembelianEntity> findQuery = mock(PanacheQuery.class);
            when(repository.find(eq("noPembelian"), any(Object[].class))).thenReturn(findQuery);
            when(findQuery.firstResult()).thenReturn(testEntity);

            TbPembelianEntity result = service.findByNoPembelian("FB001");

            assertNotNull(result);
            assertEquals("FB-20240101-1", result.getNoPembelian());
        }

        @Test
        @DisplayName("returns null when not found")
        void notFound() {
            PanacheQuery<TbPembelianEntity> findQuery = mock(PanacheQuery.class);
            when(repository.find(eq("noPembelian"), any(Object[].class))).thenReturn(findQuery);
            when(findQuery.firstResult()).thenReturn(null);

            TbPembelianEntity result = service.findByNoPembelian("NONE");

            assertNull(result);
        }
    }

    // ─── setNoUrutFromNoPembelian (tested indirectly via create) ─────

    @Nested
    @DisplayName("setNoUrutFromNoPembelian (indirect)")
    class SetNoUrutFromNoPembelian {

        @Test
        @DisplayName("parses noUrut from hyphen-separated string")
        void parsesHyphenSeparated() {
            TbPembelianEntity entity = new TbPembelianEntity();
            entity.setNoPembelian("FS-20240615-10");

            service.create(entity);

            assertEquals(10, entity.getNoUrut());
        }

        @Test
        @DisplayName("handles string with only 2 parts — noUrut stays null")
        void twoPartsNoUrutStaysNull() {
            TbPembelianEntity entity = new TbPembelianEntity();
            entity.setNoPembelian("FS-20240615");

            service.create(entity);

            assertNull(entity.getNoUrut());
        }

        @Test
        @DisplayName("handles invalid integer in third part")
        void invalidIntegerIgnored() {
            TbPembelianEntity entity = new TbPembelianEntity();
            entity.setNoPembelian("FS-20240615-XYZ");

            service.create(entity);

            assertNull(entity.getNoUrut());
        }
    }
}
