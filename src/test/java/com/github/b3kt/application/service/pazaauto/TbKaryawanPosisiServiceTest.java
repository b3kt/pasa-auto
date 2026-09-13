package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbKaryawanPosisiEntity;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbKaryawanPosisiRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TbKaryawanPosisiServiceTest {

    @Mock
    TbKaryawanPosisiRepository repository;

    @Mock
    PanacheQuery<TbKaryawanPosisiEntity> query;

    @InjectMocks
    TbKaryawanPosisiService service;

    @Test
    @DisplayName("findPaginated with search")
    void findPaginatedWithSearch() {
        PageRequest pr = new PageRequest(1, 10);
        pr.setSearch("mekanik");
        when(repository.find(anyString(), any(Object[].class))).thenReturn(query);
        when(query.count()).thenReturn(1L);
        when(query.page(any(Page.class))).thenReturn(query);
        when(query.list()).thenReturn(List.of(new TbKaryawanPosisiEntity()));

        PageResponse<TbKaryawanPosisiEntity> result = service.findPaginated(pr);
        assertEquals(1, result.getRowsNumber());
    }

    @Test
    @DisplayName("findPaginated without search")
    void findPaginatedNoSearch() {
        PageRequest pr = new PageRequest(1, 10);
        when(repository.findAll()).thenReturn(query);
        when(query.count()).thenReturn(1L);
        when(query.page(any(Page.class))).thenReturn(query);
        when(query.list()).thenReturn(List.of(new TbKaryawanPosisiEntity()));

        PageResponse<TbKaryawanPosisiEntity> result = service.findPaginated(pr);
        assertEquals(1, result.getRowsNumber());
    }

    @Test
    @DisplayName("findPaginated with sort")
    void findPaginatedWithSort() {
        PageRequest pr = new PageRequest(1, 10);
        pr.setSortBy("namaPosisi");
        when(repository.findAll()).thenReturn(query);
        when(repository.findAll(any(Sort.class))).thenReturn(query);
        when(query.count()).thenReturn(1L);
        when(query.page(any(Page.class))).thenReturn(query);
        when(query.list()).thenReturn(List.of(new TbKaryawanPosisiEntity()));

        PageResponse<TbKaryawanPosisiEntity> result = service.findPaginated(pr);
        assertEquals(1, result.getRowsNumber());
    }

    @Test
    @DisplayName("findAll delegates to repository")
    void testFindAll() {
        TbKaryawanPosisiEntity posisi = new TbKaryawanPosisiEntity();
        when(repository.listAll()).thenReturn(List.of(posisi));
        assertEquals(1, service.findAll().size());
    }

    @Test
    @DisplayName("findById returns entity when found")
    void testFindById_found() {
        TbKaryawanPosisiEntity posisi = new TbKaryawanPosisiEntity();
        when(repository.findByIdOptional(1L)).thenReturn(Optional.of(posisi));
        assertNotNull(service.findById(1L));
    }

    @Test
    @DisplayName("findById throws when not found")
    void testFindById_notFound() {
        when(repository.findByIdOptional(999L)).thenReturn(Optional.empty());
        assertThrows(jakarta.persistence.EntityNotFoundException.class, () -> service.findById(999L));
    }

    @Test
    @DisplayName("create persists entity")
    void testCreate() {
        TbKaryawanPosisiEntity posisi = new TbKaryawanPosisiEntity();
        doNothing().when(repository).persist(any(TbKaryawanPosisiEntity.class));
        assertNotNull(service.create(posisi));
    }

    @Test
    @DisplayName("update merges entity when found")
    void testUpdate_found() {
        TbKaryawanPosisiEntity posisi = new TbKaryawanPosisiEntity();
        when(repository.findByIdOptional(1L)).thenReturn(Optional.of(posisi));
        jakarta.persistence.EntityManager em = mock(jakarta.persistence.EntityManager.class);
        when(repository.getEntityManager()).thenReturn(em);
        when(em.merge(any(TbKaryawanPosisiEntity.class))).thenReturn(posisi);
        assertNotNull(service.update(1L, posisi));
    }

    @Test
    @DisplayName("update throws when not found")
    void testUpdate_notFound() {
        TbKaryawanPosisiEntity posisi = new TbKaryawanPosisiEntity();
        when(repository.findByIdOptional(999L)).thenReturn(Optional.empty());
        assertThrows(jakarta.persistence.EntityNotFoundException.class, () -> service.update(999L, posisi));
    }

    @Test
    @DisplayName("delete calls deleteById")
    void testDelete() {
        when(repository.deleteById(1L)).thenReturn(true);
        service.delete(1L);
        verify(repository).deleteById(1L);
    }
}
