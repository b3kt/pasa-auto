package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.application.dto.pazaauto.RekapPenjualanDto;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSpkEntity;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbKaryawanRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbSpkDetailRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbSpkRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TbSpkService Tests")
class TbSpkServiceTest {

    @Mock
    private TbSpkRepository repository;

    @Mock
    private TbKaryawanRepository karyawanRepository;

    @Mock
    private TbPelangganService pelangganService;

    @Mock
    private TbSpkDetailRepository detailRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private PanacheQuery<TbSpkEntity> panacheQuery;

    private TbSpkService spkService;

    private TbSpkEntity testSpkEntity;

    @BeforeEach
    void setUp() {
        spkService = new TbSpkService(
                repository,
                karyawanRepository,
                pelangganService,
                detailRepository,
                entityManager
        );

        testSpkEntity = new TbSpkEntity();
        testSpkEntity.setId(1L);
        testSpkEntity.setNoSpk("SPK202401001");
        testSpkEntity.setNopol("B1234XYZ");
        testSpkEntity.setStatusSpk("MENUNGGU");
    }

    @Test
    @DisplayName("Should find SPK by ID and populate details")
    void testFindById() {
        when(repository.findByIdOptional(1L)).thenReturn(java.util.Optional.of(testSpkEntity));
        when(detailRepository.find("id.noSpk", "SPK202401001")).thenReturn(Collections.emptyList());

        TbSpkEntity result = spkService.findById(1L);

        assertNotNull(result);
        assertEquals("SPK202401001", result.getNoSpk());
        verify(repository).findByIdOptional(1L);
        verify(detailRepository).find("id.noSpk", "SPK202401001");
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when SPK not found")
    void testFindByIdNotFound() {
        when(repository.findByIdOptional(999L)).thenReturn(java.util.Optional.empty());

        assertThrows(jakarta.persistence.EntityNotFoundException.class,
                () -> spkService.findById(999L));

        verify(repository).findByIdOptional(999L);
    }

    @Test
    @DisplayName("Should find all unprocessed SPK")
    void testFindUnprocessedSpk() {
        List<TbSpkEntity> unprocessedList = Arrays.asList(testSpkEntity);
        when(repository.find(anyString())).thenReturn(panacheQuery);
        when(panacheQuery.list()).thenReturn(unprocessedList);

        List<TbSpkEntity> result = spkService.findUnprocessedSpk();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(repository).find(contains("statusSpk"));
    }

    @Test
    @DisplayName("Should find SPK by noSpk number")
    void testFindByNoSpk() {
        when(repository.find("noSpk", "SPK202401001")).thenReturn(panacheQuery);
        when(panacheQuery.firstResult()).thenReturn(testSpkEntity);

        TbSpkEntity result = spkService.findByNoSpk("SPK202401001");

        assertNotNull(result);
        assertEquals("SPK202401001", result.getNoSpk());
        verify(repository).find("noSpk", "SPK202401001");
    }

    @Test
    @DisplayName("Should return null when no SPK found by noSpk")
    void testFindByNoSpkNotFound() {
        when(repository.find("noSpk", "NONEXISTENT")).thenReturn(panacheQuery);
        when(panacheQuery.firstResult()).thenReturn(null);

        TbSpkEntity result = spkService.findByNoSpk("NONEXISTENT");

        assertNull(result);
    }

    @Test
    @DisplayName("Should get next SPK number")
    void testGetNextSpkNumber() {
        when(repository.find(anyString(), any())).thenReturn(panacheQuery);
        when(panacheQuery.list()).thenReturn(Arrays.asList(testSpkEntity));

        String result = spkService.getNextSpkNumber("SPK202401");

        assertNotNull(result);
        assertTrue(result.startsWith("SPK202401"));
    }

    @Test
    @DisplayName("Should return default number when no existing SPK found")
    void testGetNextSpkNumberNoExisting() {
        when(repository.find(anyString(), any())).thenReturn(panacheQuery);
        when(panacheQuery.list()).thenReturn(Collections.emptyList());

        String result = spkService.getNextSpkNumber("SPK202401");

        assertNotNull(result);
        assertEquals("SPK20240100", result);
    }

    @Test
    @DisplayName("Should find paginated SPK with search filter")
    void testFindPaginatedWithSearch() {
        PageRequest pageRequest = new PageRequest(1, 10);
        pageRequest.setSearch("B1234");

        when(repository.find(anyString(), any())).thenReturn(panacheQuery);
        when(panacheQuery.count()).thenReturn(1L);
        when(panacheQuery.page(any())).thenReturn(panacheQuery);
        when(panacheQuery.list()).thenReturn(Arrays.asList(testSpkEntity));

        PageResponse<TbSpkEntity> result = spkService.findPaginated(pageRequest);

        assertNotNull(result);
        assertEquals(1, result.getTotalRecords());
        verify(repository).find(contains("B1234"));
    }

    @Test
    @DisplayName("Should find paginated SPK with status filter")
    void testFindPaginatedWithStatusFilter() {
        PageRequest pageRequest = new PageRequest(1, 10);
        pageRequest.setStatusFilter("MENUNGGU,PROSES");

        when(repository.find(anyString(), any())).thenReturn(panacheQuery);
        when(panacheQuery.count()).thenReturn(1L);
        when(panacheQuery.page(any())).thenReturn(panacheQuery);
        when(panacheQuery.list()).thenReturn(Arrays.asList(testSpkEntity));

        PageResponse<TbSpkEntity> result = spkService.findPaginated(pageRequest);

        assertNotNull(result);
        assertEquals(1, result.getTotalRecords());
    }

    @Test
    @DisplayName("Should find paginated SPK with date range filter")
    void testFindPaginatedWithDateRange() {
        PageRequest pageRequest = new PageRequest(1, 10);
        pageRequest.setStartDate("2024-01-01");
        pageRequest.setEndDate("2024-01-31");

        when(repository.find(anyString(), any())).thenReturn(panacheQuery);
        when(panacheQuery.count()).thenReturn(0L);
        when(panacheQuery.page(any())).thenReturn(panacheQuery);
        when(panacheQuery.list()).thenReturn(Collections.emptyList());

        PageResponse<TbSpkEntity> result = spkService.findPaginated(pageRequest);

        assertNotNull(result);
        assertEquals(0, result.getTotalRecords());
    }

    @Test
    @DisplayName("Should return empty result when no SPK found with filters")
    void testFindPaginatedNoResults() {
        PageRequest pageRequest = new PageRequest(1, 10);
        pageRequest.setSearch("NONEXISTENT");

        when(repository.find(anyString(), any())).thenReturn(panacheQuery);
        when(panacheQuery.count()).thenReturn(0L);
        when(panacheQuery.page(any())).thenReturn(panacheQuery);
        when(panacheQuery.list()).thenReturn(Collections.emptyList());

        PageResponse<TbSpkEntity> result = spkService.findPaginated(pageRequest);

        assertNotNull(result);
        assertEquals(0, result.getTotalRecords());
        assertTrue(result.getRows().isEmpty());
    }
}
