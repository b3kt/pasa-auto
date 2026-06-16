package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbBarangEntity;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbBarangRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TbBarangServiceTest {

    @Mock
    TbBarangRepository repository;

    @Mock
    PanacheQuery<TbBarangEntity> query;

    @InjectMocks
    TbBarangService tbBarangService;

    private TbBarangEntity testEntity;

    @BeforeEach
    void setUp() {
        testEntity = new TbBarangEntity();
        testEntity.setId(1L);
        testEntity.setNamaBarang("Oli Mesin");
    }

    @Test
    @DisplayName("findAll returns list")
    void findAll() {
        when(repository.listAll()).thenReturn(List.of(testEntity));
        assertEquals(1, tbBarangService.findAll().size());
    }

    @Test
    @DisplayName("search with keyword")
    void searchWithKeyword() {
        when(repository.find(anyString(), any(Sort.class), anyString())).thenReturn(query);
        when(query.list()).thenReturn(List.of(testEntity));

        List<TbBarangEntity> result = tbBarangService.search("oli");
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("search with null returns all")
    void searchNull() {
        when(repository.listAll()).thenReturn(List.of(testEntity));
        List<TbBarangEntity> result = tbBarangService.search(null);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("search with empty string returns all")
    void searchEmpty() {
        when(repository.listAll()).thenReturn(List.of(testEntity));
        List<TbBarangEntity> result = tbBarangService.search("");
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("findPaginated without filters")
    void findPaginatedNoFilters() {
        PageRequest pr = new PageRequest(1, 10);
        when(repository.find(anyString())).thenReturn(query);
        when(query.count()).thenReturn(1L);
        when(query.page(any(Page.class))).thenReturn(query);
        when(query.list()).thenReturn(List.of(testEntity));

        PageResponse<TbBarangEntity> result = tbBarangService.findPaginated(pr);
        assertEquals(1, result.getRowsNumber());
    }

    @Test
    @DisplayName("findPaginated with search")
    void findPaginatedWithSearch() {
        PageRequest pr = new PageRequest(1, 10);
        pr.setSearch("oli");
        when(repository.find(anyString(), any(Object[].class))).thenReturn(query);
        when(query.count()).thenReturn(1L);
        when(query.page(any(Page.class))).thenReturn(query);
        when(query.list()).thenReturn(List.of(testEntity));

        PageResponse<TbBarangEntity> result = tbBarangService.findPaginated(pr);
        assertEquals(1, result.getRowsNumber());
    }

    @Test
    @DisplayName("findPaginated with status filter AVAILABLE")
    void findPaginatedStatusAvailable() {
        PageRequest pr = new PageRequest(1, 10);
        pr.setStatusFilter("AVAILABLE");
        when(repository.find(anyString())).thenReturn(query);
        when(query.count()).thenReturn(1L);
        when(query.page(any(Page.class))).thenReturn(query);
        when(query.list()).thenReturn(List.of(testEntity));

        PageResponse<TbBarangEntity> result = tbBarangService.findPaginated(pr);
        assertEquals(1, result.getRowsNumber());
    }

    @Test
    @DisplayName("findPaginated with status filter OUT_OF_STOCK")
    void findPaginatedStatusOutOfStock() {
        PageRequest pr = new PageRequest(1, 10);
        pr.setStatusFilter("OUT_OF_STOCK");
        when(repository.find(anyString())).thenReturn(query);
        when(query.count()).thenReturn(1L);
        when(query.page(any(Page.class))).thenReturn(query);
        when(query.list()).thenReturn(List.of(testEntity));

        PageResponse<TbBarangEntity> result = tbBarangService.findPaginated(pr);
        assertEquals(1, result.getRowsNumber());
    }

    @Test
    @DisplayName("create persists entity")
    void create() {
        doNothing().when(repository).persist(any(TbBarangEntity.class));
        assertNotNull(tbBarangService.create(testEntity));
    }
}
