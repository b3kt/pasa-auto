package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbKendaraanEntity;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbKendaraanRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
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
class TbKendaraanServiceTest {

    @Mock
    TbKendaraanRepository repository;

    @Mock
    PanacheQuery<TbKendaraanEntity> query;

    @InjectMocks
    TbKendaraanService service;

    private TbKendaraanEntity kendaraan;

    @BeforeEach
    void setUp() {
        kendaraan = new TbKendaraanEntity();
        kendaraan.setId(1L);
        kendaraan.setJenis("Sedan");
        kendaraan.setMerk("Toyota");
        kendaraan.setModel("Corolla");
    }

    @Test
    @DisplayName("findPaginated with search filters by jenis/merk")
    void testFindPaginated_withSearch() {
        PageRequest pr = new PageRequest(1, 10);
        pr.setSearch("toyota");
        when(repository.find(anyString(), any(Object[].class))).thenReturn(query);
        when(query.count()).thenReturn(1L);
        when(query.page(any(Page.class))).thenReturn(query);
        when(query.list()).thenReturn(List.of(kendaraan));

        PageResponse<TbKendaraanEntity> result = service.findPaginated(pr);

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
        when(query.list()).thenReturn(List.of(kendaraan));

        PageResponse<TbKendaraanEntity> result = service.findPaginated(pr);

        assertEquals(1, result.getRows().size());
        assertEquals(1, result.getRowsNumber());
    }

    @Test
    @DisplayName("findDistinctMerks delegates to repository")
    void testFindDistinctMerks() {
        when(repository.findDistinctMerk()).thenReturn(List.of("Toyota", "Honda"));

        List<String> result = service.findDistinctMerks();

        assertEquals(2, result.size());
        assertEquals("Toyota", result.get(0));
        verify(repository).findDistinctMerk();
    }

    @Test
    @DisplayName("findDistinctJenis delegates to repository")
    void testFindDistinctJenis() {
        when(repository.findDistinctJenis()).thenReturn(List.of("Sedan", "SUV"));

        List<String> result = service.findDistinctJenis();

        assertEquals(2, result.size());
        verify(repository).findDistinctJenis();
    }

    @Test
    @DisplayName("findDistinctJenisByMerk delegates to repository")
    void testFindDistinctJenisByMerk() {
        when(repository.findDistinctJenisByMerk("Toyota")).thenReturn(List.of("Sedan", "SUV"));

        List<String> result = service.findDistinctJenisByMerk("Toyota");

        assertEquals(2, result.size());
        verify(repository).findDistinctJenisByMerk("Toyota");
    }

    @Test
    @DisplayName("findAll delegates to repository listAll")
    void testFindAll() {
        when(repository.listAll()).thenReturn(List.of(kendaraan));
        assertEquals(1, service.findAll().size());
    }

    @Test
    @DisplayName("findById returns entity when found")
    void testFindById_found() {
        when(repository.findByIdOptional(1L)).thenReturn(Optional.of(kendaraan));
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
        doNothing().when(repository).persist(any(TbKendaraanEntity.class));
        assertNotNull(service.create(kendaraan));
    }

    @Test
    @DisplayName("update merges entity when found")
    void testUpdate_found() {
        when(repository.findByIdOptional(1L)).thenReturn(Optional.of(kendaraan));
        jakarta.persistence.EntityManager em = mock(jakarta.persistence.EntityManager.class);
        when(repository.getEntityManager()).thenReturn(em);
        when(em.merge(any(TbKendaraanEntity.class))).thenReturn(kendaraan);
        assertNotNull(service.update(1L, kendaraan));
    }

    @Test
    @DisplayName("update throws when not found")
    void testUpdate_notFound() {
        when(repository.findByIdOptional(999L)).thenReturn(Optional.empty());
        assertThrows(jakarta.persistence.EntityNotFoundException.class, () -> service.update(999L, kendaraan));
    }

    @Test
    @DisplayName("delete calls deleteById")
    void testDelete() {
        when(repository.deleteById(1L)).thenReturn(true);
        service.delete(1L);
        verify(repository).deleteById(1L);
    }
}
