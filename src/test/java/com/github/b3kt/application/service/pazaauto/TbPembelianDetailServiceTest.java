package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPembelianDetailEntity;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbPembelianDetailRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
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
class TbPembelianDetailServiceTest {

    @Mock
    TbPembelianDetailRepository repository;

    @InjectMocks
    TbPembelianDetailService service;

    @Test
    @DisplayName("findByPembelianId delegates to repository")
    void findByPembelianId() {
        TbPembelianDetailEntity detail = new TbPembelianDetailEntity();
        when(repository.findByPembelianId(1L)).thenReturn(List.of(detail));
        assertEquals(1, service.findByPembelianId(1L).size());
    }

    @Test
    @DisplayName("deleteByPembelianId delegates to repository")
    void deleteByPembelianId() {
        service.deleteByPembelianId(1L);
        verify(repository).deleteByPembelianId(1L);
    }

    @Test
    @DisplayName("saveAll persists each detail")
    void saveAll() {
        TbPembelianDetailEntity d1 = new TbPembelianDetailEntity();
        TbPembelianDetailEntity d2 = new TbPembelianDetailEntity();
        service.saveAll(List.of(d1, d2));
        verify(repository, times(2)).persist(any(TbPembelianDetailEntity.class));
    }

    @Test
    @DisplayName("findAll delegates to repository")
    void testFindAll() {
        TbPembelianDetailEntity detail = new TbPembelianDetailEntity();
        when(repository.listAll()).thenReturn(List.of(detail));
        assertEquals(1, service.findAll().size());
    }

    @Test
    @DisplayName("findById returns entity when found")
    void testFindById_found() {
        TbPembelianDetailEntity detail = new TbPembelianDetailEntity();
        when(repository.findByIdOptional(1L)).thenReturn(Optional.of(detail));
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
        TbPembelianDetailEntity detail = new TbPembelianDetailEntity();
        doNothing().when(repository).persist(any(TbPembelianDetailEntity.class));
        assertNotNull(service.create(detail));
    }

    @Test
    @DisplayName("update merges entity when found")
    void testUpdate_found() {
        TbPembelianDetailEntity detail = new TbPembelianDetailEntity();
        when(repository.findByIdOptional(1L)).thenReturn(Optional.of(detail));
        jakarta.persistence.EntityManager em = mock(jakarta.persistence.EntityManager.class);
        when(repository.getEntityManager()).thenReturn(em);
        when(em.merge(any(TbPembelianDetailEntity.class))).thenReturn(detail);
        assertNotNull(service.update(1L, detail));
    }

    @Test
    @DisplayName("update throws when not found")
    void testUpdate_notFound() {
        TbPembelianDetailEntity detail = new TbPembelianDetailEntity();
        when(repository.findByIdOptional(999L)).thenReturn(Optional.empty());
        assertThrows(jakarta.persistence.EntityNotFoundException.class, () -> service.update(999L, detail));
    }

    @Test
    @DisplayName("delete calls deleteById")
    void testDelete() {
        when(repository.deleteById(1L)).thenReturn(true);
        service.delete(1L);
        verify(repository).deleteById(1L);
    }
}
