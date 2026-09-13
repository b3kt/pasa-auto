package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSparepartEntity;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbSparepartRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TbSparepartServiceTest {

    @Mock
    TbSparepartRepository repository;

    @Mock
    PanacheQuery<TbSparepartEntity> query;

    @InjectMocks
    TbSparepartService service;

    private TbSparepartEntity sparepart;

    @BeforeEach
    void setUp() {
        sparepart = new TbSparepartEntity();
        sparepart.setId(1L);
        sparepart.setNamaSparepart("Oli Mesin");
        sparepart.setKodeSparepart("OLI001");
        sparepart.setStok(10);
        sparepart.setHargaJual(new BigDecimal("50000"));
        sparepart.setSupplierId(1L);
    }

    @Test
    @DisplayName("findPaginated with no filters builds empty query")
    void testFindPaginated_noFilters() {
        PageRequest pr = new PageRequest(1, 10);
        when(repository.find(anyString(), any(Map.class))).thenReturn(query);
        when(query.count()).thenReturn(1L);
        when(query.page(any(Page.class))).thenReturn(query);
        when(query.list()).thenReturn(List.of(sparepart));

        PageResponse<TbSparepartEntity> result = service.findPaginated(pr);

        assertEquals(1, result.getRows().size());
        assertEquals(1, result.getRowsNumber());
    }

    @Test
    @DisplayName("findPaginated with search filter")
    void testFindPaginated_withSearch() {
        PageRequest pr = new PageRequest(1, 10);
        pr.setSearch("oli");
        when(repository.find(anyString(), any(Map.class))).thenReturn(query);
        when(query.count()).thenReturn(1L);
        when(query.page(any(Page.class))).thenReturn(query);
        when(query.list()).thenReturn(List.of(sparepart));

        PageResponse<TbSparepartEntity> result = service.findPaginated(pr);

        assertEquals(1, result.getRows().size());
        verify(repository).find(argThat(q -> q.contains("namaSparepart")), any(Map.class));
    }

    @Test
    @DisplayName("findPaginated with supplierId filter")
    void testFindPaginated_withSupplierId() {
        PageRequest pr = new PageRequest(1, 10);
        pr.setSupplierId(1L);
        when(repository.find(anyString(), any(Map.class))).thenReturn(query);
        when(query.count()).thenReturn(1L);
        when(query.page(any(Page.class))).thenReturn(query);
        when(query.list()).thenReturn(List.of(sparepart));

        PageResponse<TbSparepartEntity> result = service.findPaginated(pr);

        assertEquals(1, result.getRows().size());
        verify(repository).find(argThat(q -> q.contains("supplierId")), any(Map.class));
    }

    @Test
    @DisplayName("findPaginated with both search and supplierId")
    void testFindPaginated_withBoth() {
        PageRequest pr = new PageRequest(1, 10);
        pr.setSearch("oli");
        pr.setSupplierId(1L);
        when(repository.find(anyString(), any(Map.class))).thenReturn(query);
        when(query.count()).thenReturn(1L);
        when(query.page(any(Page.class))).thenReturn(query);
        when(query.list()).thenReturn(List.of(sparepart));

        PageResponse<TbSparepartEntity> result = service.findPaginated(pr);

        assertEquals(1, result.getRows().size());
        verify(repository).find(argThat(q -> q.contains("namaSparepart") && q.contains("supplierId")), any(Map.class));
    }

    @Test
    @DisplayName("increaseStock adds quantity to current stock")
    void testIncreaseStock() {
        when(repository.findByIdOptional(1L)).thenReturn(Optional.of(sparepart));
        doNothing().when(repository).persist(any(TbSparepartEntity.class));

        service.increaseStock(1L, 5);

        assertEquals(15, sparepart.getStok());
        verify(repository).persist(sparepart);
    }

    @Test
    @DisplayName("decreaseStock reduces stock successfully")
    void testDecreaseStock_success() {
        sparepart.setStok(10);
        when(repository.findByIdOptional(1L)).thenReturn(Optional.of(sparepart));
        doNothing().when(repository).persist(any(TbSparepartEntity.class));

        service.decreaseStock(1L, 3);

        assertEquals(7, sparepart.getStok());
        verify(repository).persist(sparepart);
    }

    @Test
    @DisplayName("decreaseStock throws when stock would go below zero")
    void testDecreaseStock_belowZero_throws() {
        sparepart.setStok(5);
        when(repository.findByIdOptional(1L)).thenReturn(Optional.of(sparepart));

        assertThrows(IllegalStateException.class, () -> service.decreaseStock(1L, 10));
        verify(repository, never()).persist(any(TbSparepartEntity.class));
    }

    @Test
    @DisplayName("decreaseStock treats null stock as zero")
    void testDecreaseStock_nullStock_treatsAsZero() {
        sparepart.setStok(null);
        when(repository.findByIdOptional(1L)).thenReturn(Optional.of(sparepart));

        assertThrows(IllegalStateException.class, () -> service.decreaseStock(1L, 1));
    }

    @Test
    @DisplayName("increaseStock treats null stock as zero")
    void testIncreaseStock_nullStock() {
        sparepart.setStok(null);
        when(repository.findByIdOptional(1L)).thenReturn(Optional.of(sparepart));
        doNothing().when(repository).persist(any(TbSparepartEntity.class));

        service.increaseStock(1L, 5);

        assertEquals(5, sparepart.getStok());
    }
}
