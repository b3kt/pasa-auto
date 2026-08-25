package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSupplierEntity;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbSupplierRepository;
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
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TbSupplierServiceTest {

    @Mock
    TbSupplierRepository repository;

    @Mock
    PanacheQuery<TbSupplierEntity> query;

    @InjectMocks
    TbSupplierService service;

    private TbSupplierEntity supplier;

    @BeforeEach
    void setUp() {
        supplier = new TbSupplierEntity();
        supplier.setId(1);
        supplier.setNamaSupplier("Supplier ABC");
        supplier.setEmail("abc@example.com");
    }

    @Test
    @DisplayName("findPaginated with search filters by nama/email")
    void testFindPaginated_withSearch() {
        PageRequest pr = new PageRequest(1, 10);
        pr.setSearch("abc");
        when(repository.find(anyString(), any(Object[].class))).thenReturn(query);
        when(query.count()).thenReturn(1L);
        when(query.page(any(Page.class))).thenReturn(query);
        when(query.list()).thenReturn(List.of(supplier));

        PageResponse<TbSupplierEntity> result = service.findPaginated(pr);

        assertEquals(1, result.getRows().size());
        assertEquals(1, result.getRowsNumber());
    }

    @Test
    @DisplayName("findPaginated without search returns all")
    void testFindPaginated_noSearch() {
        PageRequest pr = new PageRequest(1, 10);
        when(repository.findAll()).thenReturn(query);
        when(query.count()).thenReturn(1L);
        when(query.page(any(Page.class))).thenReturn(query);
        when(query.list()).thenReturn(List.of(supplier));

        PageResponse<TbSupplierEntity> result = service.findPaginated(pr);

        assertEquals(1, result.getRows().size());
    }

    @Test
    @DisplayName("search with null returns all via listAll")
    void testSearch_null() {
        when(repository.listAll()).thenReturn(List.of(supplier));

        List<TbSupplierEntity> result = service.search(null);

        assertEquals(1, result.size());
        verify(repository).listAll();
        verify(repository, never()).find(anyString(), any(Sort.class), anyString());
    }

    @Test
    @DisplayName("search with empty string returns all via listAll")
    void testSearch_empty() {
        when(repository.listAll()).thenReturn(List.of(supplier));

        List<TbSupplierEntity> result = service.search("");

        assertEquals(1, result.size());
        verify(repository).listAll();
    }

    @Test
    @DisplayName("search with keyword filters results")
    void testSearch_withKeyword() {
        when(repository.find(anyString(), any(Sort.class), anyString())).thenReturn(query);
        when(query.list()).thenReturn(List.of(supplier));

        List<TbSupplierEntity> result = service.search("abc");

        assertEquals(1, result.size());
        verify(repository).find(
                argThat(q -> q.contains("namaSupplier") && q.contains("email")),
                any(Sort.class),
                eq("%abc%")
        );
    }

    @Test
    @DisplayName("findAll delegates to repository")
    void testFindAll() {
        when(repository.listAll()).thenReturn(List.of(supplier));
        assertEquals(1, service.findAll().size());
    }

    @Test
    @DisplayName("findById returns entity when found")
    void testFindById_found() {
        when(repository.findByIdOptional(1)).thenReturn(Optional.of(supplier));
        assertNotNull(service.findById(1));
    }

    @Test
    @DisplayName("findById throws when not found")
    void testFindById_notFound() {
        when(repository.findByIdOptional(999)).thenReturn(Optional.empty());
        assertThrows(jakarta.persistence.EntityNotFoundException.class, () -> service.findById(999));
    }

    @Test
    @DisplayName("create persists entity")
    void testCreate() {
        doNothing().when(repository).persist(any(TbSupplierEntity.class));
        assertNotNull(service.create(supplier));
    }

    @Test
    @DisplayName("update merges entity when found")
    void testUpdate_found() {
        when(repository.findByIdOptional(1)).thenReturn(Optional.of(supplier));
        jakarta.persistence.EntityManager em = mock(jakarta.persistence.EntityManager.class);
        when(repository.getEntityManager()).thenReturn(em);
        when(em.merge(any(TbSupplierEntity.class))).thenReturn(supplier);
        assertNotNull(service.update(1, supplier));
    }

    @Test
    @DisplayName("update throws when not found")
    void testUpdate_notFound() {
        when(repository.findByIdOptional(999)).thenReturn(Optional.empty());
        assertThrows(jakarta.persistence.EntityNotFoundException.class, () -> service.update(999, supplier));
    }

    @Test
    @DisplayName("delete calls deleteById")
    void testDelete() {
        when(repository.deleteById(1)).thenReturn(true);
        service.delete(1);
        verify(repository).deleteById(1);
    }
}
