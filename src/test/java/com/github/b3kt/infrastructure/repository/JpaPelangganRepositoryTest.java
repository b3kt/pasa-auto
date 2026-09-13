package com.github.b3kt.infrastructure.repository;

import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.domain.model.pazaauto.Pelanggan;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPelangganEntity;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbPelangganRepository;
import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Sort;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
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
class JpaPelangganRepositoryTest {

    @Mock
    TbPelangganRepository panacheRepository;

    @Mock
    Cache cache;

    @Mock
    EntityManager entityManager;

    @Mock
    PanacheQuery<TbPelangganEntity> query;

    @InjectMocks
    JpaPelangganRepository repository;

    private TbPelangganEntity testEntity;
    private Pelanggan testPelanggan;

    @BeforeEach
    void setUp() {
        testPelanggan = new Pelanggan("B1234XYZ", "Budi", "Toyota");
        testPelanggan.setId(1L);

        testEntity = new TbPelangganEntity();
        testEntity.setId(1L);
        testEntity.setNopol("B1234XYZ");
        testEntity.setNamaPelanggan("Budi");
    }

    @Test
    @DisplayName("findById returns domain when entity exists")
    void findByIdFound() {
        when(panacheRepository.findByIdOptional(1L)).thenReturn(Optional.of(testEntity));
        Optional<Pelanggan> result = repository.findById(1L);
        assertTrue(result.isPresent());
        assertEquals("B1234XYZ", result.get().getNopol());
    }

    @Test
    @DisplayName("findById returns empty when not found")
    void findByIdNotFound() {
        when(panacheRepository.findByIdOptional(999L)).thenReturn(Optional.empty());
        Optional<Pelanggan> result = repository.findById(999L);
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("findByNopol returns domain when found")
    void findByNopolFound() {
        when(panacheRepository.find(eq("nopol"), any(Sort.class), eq("B1234XYZ"))).thenReturn(query);
        when(query.firstResultOptional()).thenReturn(Optional.of(testEntity));
        Optional<Pelanggan> result = repository.findByNopol("B1234XYZ");
        assertTrue(result.isPresent());
    }

    @Test
    @DisplayName("findByNopol returns empty for null nopol")
    void findByNopolNull() {
        Optional<Pelanggan> result = repository.findByNopol(null);
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("findByNopol returns empty for blank nopol")
    void findByNopolBlank() {
        Optional<Pelanggan> result = repository.findByNopol("  ");
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("findAll returns list of domains")
    void findAll() {
        when(panacheRepository.findAll()).thenReturn(query);
        when(query.stream()).thenReturn(java.util.stream.Stream.of(testEntity));
        List<Pelanggan> result = repository.findAll();
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("findPaginated with search")
    void findPaginatedWithSearch() {
        PageRequest pr = new PageRequest(1, 10);
        pr.setSearch("Budi");
        when(panacheRepository.find(anyString(), anyString())).thenReturn(query);
        when(query.count()).thenReturn(1L);
        when(query.page(any())).thenReturn(query);
        when(query.list()).thenReturn(List.of(testEntity));

        PageResponse<Pelanggan> result = repository.findPaginated(pr);
        assertNotNull(result);
    }

    @Test
    @DisplayName("findPaginated without search")
    void findPaginatedWithoutSearch() {
        PageRequest pr = new PageRequest(1, 10);
        when(panacheRepository.findAll()).thenReturn(query);
        when(query.count()).thenReturn(1L);
        when(query.page(any())).thenReturn(query);
        when(query.list()).thenReturn(List.of(testEntity));

        PageResponse<Pelanggan> result = repository.findPaginated(pr);
        assertNotNull(result);
    }

    @Test
    @DisplayName("deleteById delegates to repository")
    void deleteById() {
        when(panacheRepository.deleteById(1L)).thenReturn(true);
        repository.deleteById(1L);
        verify(panacheRepository).deleteById(1L);
    }
}
