package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbKendaraanEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbMerkKendaraanEntity;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbKendaraanRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbMerkKendaraanRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbPelangganKendaraanRepository;
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
    TbPelangganKendaraanRepository pelangganKendaraanRepository;

    @Mock
    TbMerkKendaraanRepository merkRepository;

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
        kendaraan.setMerkId(7L);
        kendaraan.setModel("Corolla");

        TbMerkKendaraanEntity toyota = new TbMerkKendaraanEntity();
        toyota.setId(7L);
        toyota.setNama("TOYOTA");
        lenient().when(merkRepository.findByIdOptional(7L)).thenReturn(Optional.of(toyota));
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
    @DisplayName("findPaginated filters by merkId")
    void testFindPaginated_byMerkId() {
        PageRequest pr = new PageRequest(1, 10);
        when(repository.find(eq("merkId = ?1"), any(Object[].class))).thenReturn(query);
        when(query.count()).thenReturn(1L);
        when(query.page(any(Page.class))).thenReturn(query);
        when(query.list()).thenReturn(List.of(kendaraan));

        PageResponse<TbKendaraanEntity> result = service.findPaginated(pr, 5L);

        assertEquals(1, result.getRows().size());
        verify(repository).find(eq("merkId = ?1"), any(Object[].class));
    }

    @Test
    @DisplayName("create syncs deprecated merk from merk master")
    void testCreate_syncsMerk() {
        TbMerkKendaraanEntity toyota = new TbMerkKendaraanEntity();
        toyota.setId(3L);
        toyota.setNama("TOYOTA");
        when(merkRepository.findByIdOptional(3L)).thenReturn(Optional.of(toyota));
        TbKendaraanEntity input = new TbKendaraanEntity();
        input.setMerkId(3L);
        input.setJenis("Yaris");

        TbKendaraanEntity result = service.create(input);

        assertEquals("TOYOTA", result.getMerk());
        verify(repository).persist(input);
    }

    @Test
    @DisplayName("create without merkId is rejected")
    void testCreate_requiresMerk() {
        TbKendaraanEntity input = new TbKendaraanEntity();
        input.setJenis("Yaris");

        assertThrows(IllegalArgumentException.class, () -> service.create(input));
        verify(repository, never()).persist(any(TbKendaraanEntity.class));
    }

    @Test
    @DisplayName("delete calls deleteById")
    void testDelete() {
        when(pelangganKendaraanRepository.findByKendaraanIdOrderByTanggalMulai(1L)).thenReturn(List.of());
        when(repository.deleteById(1L)).thenReturn(true);
        service.delete(1L);
        verify(repository).deleteById(1L);
    }

    @Test
    @DisplayName("findOrCreateByMerkJenis returns existing master")
    void testFindOrCreateByMerkJenis_existing() {
        when(repository.findByMerkAndJenis("Toyota", "Sedan")).thenReturn(Optional.of(kendaraan));

        TbKendaraanEntity result = service.findOrCreateByMerkJenis("Toyota", "Sedan");

        assertSame(kendaraan, result);
        verify(repository, never()).getEntityManager();
    }

    @Test
    @DisplayName("findOrCreateByMerkJenis persists a new master when none exists")
    void testFindOrCreateByMerkJenis_creates() {
        when(repository.findByMerkAndJenis("Honda", "Sedan")).thenReturn(Optional.empty());
        TbMerkKendaraanEntity honda = new TbMerkKendaraanEntity();
        honda.setId(5L);
        honda.setNama("HONDA");
        when(merkRepository.findOrCreateByNama("Honda")).thenReturn(honda);
        jakarta.persistence.EntityManager em = mock(jakarta.persistence.EntityManager.class);
        when(repository.getEntityManager()).thenReturn(em);
        when(em.merge(any(TbKendaraanEntity.class))).thenAnswer(inv -> {
            TbKendaraanEntity created = inv.getArgument(0);
            created.setId(99L);
            return created;
        });

        TbKendaraanEntity result = service.findOrCreateByMerkJenis("Honda", "Sedan");

        assertEquals("HONDA", result.getMerk());
        assertEquals(5L, result.getMerkId());
        assertEquals("Sedan", result.getJenis());
        assertEquals(99L, result.getId());
        verify(em).merge(any(TbKendaraanEntity.class));
    }
}
