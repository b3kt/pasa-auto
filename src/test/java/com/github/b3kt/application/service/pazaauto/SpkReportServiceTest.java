package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.application.dto.pazaauto.RekapPenjualanDto;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSpkDetailEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSpkDetailId;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSpkEntity;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbSpkDetailRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbSpkRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SpkReportService Tests")
class SpkReportServiceTest {

    @Mock
    private TbSpkRepository repository;

    @Mock
    private TbSpkDetailRepository detailRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private SpkEnrichmentService enrichmentService;

    @Mock
    private PanacheQuery<TbSpkEntity> panacheQuery;

    @Mock
    private PanacheQuery<TbSpkDetailEntity> detailPanacheQuery;

    @Mock
    private TypedQuery<RekapPenjualanDto> typedQuery;

    @Mock
    private Query jpaQuery;

    private SpkReportService spkReportService;

    private TbSpkEntity testSpkEntity;

    @BeforeEach
    void setUp() {
        spkReportService = new SpkReportService(
                repository,
                detailRepository,
                entityManager,
                enrichmentService
        );

        testSpkEntity = new TbSpkEntity();
        testSpkEntity.setId(1L);
        testSpkEntity.setNoSpk("SPK202401001");
        testSpkEntity.setNopol("B1234XYZ");
        testSpkEntity.setStatusSpk("PROSES");
    }

    @Nested
    @DisplayName("findPaginated")
    class FindPaginatedTests {

        @Test
        @DisplayName("Should find paginated SPK without filters")
        void testFindPaginated_noFilters() {
            PageRequest pageRequest = new PageRequest(1, 10);

            when(repository.find(anyString())).thenReturn(panacheQuery);
            when(panacheQuery.count()).thenReturn(1L);
            when(panacheQuery.page(any(Page.class))).thenReturn(panacheQuery);
            when(panacheQuery.list()).thenReturn(List.of(testSpkEntity));

            PageResponse<TbSpkEntity> result = spkReportService.findPaginated(pageRequest);

            assertNotNull(result);
            assertEquals(1, result.getRows().size());
            assertEquals("SPK202401001", result.getRows().get(0).getNoSpk());
            assertEquals(1L, result.getRowsNumber());
            verify(enrichmentService).fillRequiredFields(anyList());
        }

        @Test
        @DisplayName("Should find paginated SPK with search filter")
        void testFindPaginated_withSearch() {
            PageRequest pageRequest = new PageRequest(1, 10);
            pageRequest.setSearch("SPK");

            when(repository.find(anyString(), any(Object[].class))).thenReturn(panacheQuery);
            when(panacheQuery.count()).thenReturn(1L);
            when(panacheQuery.page(any(Page.class))).thenReturn(panacheQuery);
            when(panacheQuery.list()).thenReturn(List.of(testSpkEntity));

            PageResponse<TbSpkEntity> result = spkReportService.findPaginated(pageRequest);

            assertNotNull(result);
            assertEquals(1, result.getRows().size());
            verify(repository).find(anyString(), any(Object[].class));
        }

        @Test
        @DisplayName("Should find paginated SPK with sortBy")
        void testFindPaginated_withSort() {
            PageRequest pageRequest = new PageRequest(1, 10);
            pageRequest.setSortBy("noSpk");
            pageRequest.setDescending(true);

            when(repository.find(anyString(), any(Sort.class), any(Object[].class))).thenReturn(panacheQuery);
            when(panacheQuery.count()).thenReturn(1L);
            when(panacheQuery.page(any(Page.class))).thenReturn(panacheQuery);
            when(panacheQuery.list()).thenReturn(List.of(testSpkEntity));

            PageResponse<TbSpkEntity> result = spkReportService.findPaginated(pageRequest);

            assertNotNull(result);
            verify(repository).find(anyString(), any(Sort.class), any(Object[].class));
        }

        @Test
        @DisplayName("Should find paginated SPK with all filters combined")
        void testFindPaginated_allFilters() {
            PageRequest pageRequest = new PageRequest(2, 5);
            pageRequest.setSearch("B1234");
            pageRequest.setStatusFilter("PROSES");
            pageRequest.setStartDate("2024-01-01");
            pageRequest.setEndDate("2024-01-31");
            pageRequest.setSortBy("statusSpk");
            pageRequest.setDescending(false);

            when(repository.find(anyString(), any(Sort.class), any(Object[].class))).thenReturn(panacheQuery);
            when(panacheQuery.count()).thenReturn(2L);
            when(panacheQuery.page(any(Page.class))).thenReturn(panacheQuery);
            when(panacheQuery.list()).thenReturn(List.of(testSpkEntity));

            PageResponse<TbSpkEntity> result = spkReportService.findPaginated(pageRequest);

            assertNotNull(result);
            verify(enrichmentService).fillRequiredFields(anyList());
        }

        @Test
        @DisplayName("Should find paginated SPK with ascending sort")
        void testFindPaginated_withAscendingSort() {
            PageRequest pageRequest = new PageRequest(1, 10);
            pageRequest.setSortBy("noSpk");
            pageRequest.setDescending(false);

            when(repository.find(anyString(), any(Sort.class), any(Object[].class))).thenReturn(panacheQuery);
            when(panacheQuery.count()).thenReturn(1L);
            when(panacheQuery.page(any(Page.class))).thenReturn(panacheQuery);
            when(panacheQuery.list()).thenReturn(List.of(testSpkEntity));

            PageResponse<TbSpkEntity> result = spkReportService.findPaginated(pageRequest);

            assertNotNull(result);
            verify(repository).find(anyString(), any(Sort.class), any(Object[].class));
        }
    }

    @Nested
    @DisplayName("findPaginatedWithPenjualan")
    class FindPaginatedWithPenjualanTests {

        @Test
        @DisplayName("Should find paginated with penjualan without filters")
        void testFindPaginatedWithPenjualan_noFilters() {
            PageRequest pageRequest = new PageRequest(1, 10);

            when(entityManager.createQuery(anyString(), eq(RekapPenjualanDto.class))).thenReturn(typedQuery);
            when(entityManager.createQuery(anyString())).thenReturn(jpaQuery);
            when(jpaQuery.getSingleResult()).thenReturn(1L);
            when(typedQuery.getResultList()).thenReturn(List.of(new RekapPenjualanDto()));

            PageResponse<RekapPenjualanDto> result = spkReportService.findPaginatedWithPenjualan(pageRequest);

            assertNotNull(result);
            assertEquals(1, result.getRows().size());
            verify(typedQuery).setFirstResult(0);
            verify(typedQuery).setMaxResults(10);
        }

        @Test
        @DisplayName("Should find paginated with penjualan with search")
        void testFindPaginatedWithPenjualan_withSearch() {
            PageRequest pageRequest = new PageRequest(1, 10);
            pageRequest.setSearch("SPK");

            when(entityManager.createQuery(anyString(), eq(RekapPenjualanDto.class))).thenReturn(typedQuery);
            when(entityManager.createQuery(anyString())).thenReturn(jpaQuery);
            when(jpaQuery.getSingleResult()).thenReturn(0L);
            when(typedQuery.getResultList()).thenReturn(Collections.emptyList());

            PageResponse<RekapPenjualanDto> result = spkReportService.findPaginatedWithPenjualan(pageRequest);

            assertNotNull(result);
            assertEquals(0, result.getRowsNumber());
        }

        @Test
        @DisplayName("Should handle grandTotal sort")
        void testFindPaginatedWithPenjualan_sortByGrandTotal() {
            PageRequest pageRequest = new PageRequest(1, 10);
            pageRequest.setSortBy("grandTotal");
            pageRequest.setDescending(true);

            when(entityManager.createQuery(contains("order by p.grandTotal desc"), eq(RekapPenjualanDto.class))).thenReturn(typedQuery);
            when(entityManager.createQuery(contains("COUNT"))).thenReturn(jpaQuery);
            when(jpaQuery.getSingleResult()).thenReturn(0L);
            when(typedQuery.getResultList()).thenReturn(List.of());

            PageResponse<RekapPenjualanDto> result = spkReportService.findPaginatedWithPenjualan(pageRequest);

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should handle non-grandTotal sort (SPK field)")
        void testFindPaginatedWithPenjualan_sortByNoSpk() {
            PageRequest pageRequest = new PageRequest(1, 10);
            pageRequest.setSortBy("noSpk");
            pageRequest.setDescending(false);

            when(entityManager.createQuery(contains("order by s.noSpk asc"), eq(RekapPenjualanDto.class))).thenReturn(typedQuery);
            when(entityManager.createQuery(contains("COUNT"))).thenReturn(jpaQuery);
            when(jpaQuery.getSingleResult()).thenReturn(0L);
            when(typedQuery.getResultList()).thenReturn(List.of());

            PageResponse<RekapPenjualanDto> result = spkReportService.findPaginatedWithPenjualan(pageRequest);

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should handle status filter")
        void testFindPaginatedWithPenjualan_withStatusFilter() {
            PageRequest pageRequest = new PageRequest(1, 10);
            pageRequest.setStatusFilter("PROSES,SELESAI");

            when(entityManager.createQuery(anyString(), eq(RekapPenjualanDto.class))).thenReturn(typedQuery);
            when(entityManager.createQuery(anyString())).thenReturn(jpaQuery);
            when(jpaQuery.getSingleResult()).thenReturn(5L);
            when(typedQuery.getResultList()).thenReturn(List.of(new RekapPenjualanDto()));

            PageResponse<RekapPenjualanDto> result = spkReportService.findPaginatedWithPenjualan(pageRequest);

            assertNotNull(result);
            assertEquals(5L, result.getRowsNumber());
        }

        @Test
        @DisplayName("Should handle date range filter")
        void testFindPaginatedWithPenjualan_withDateRange() {
            PageRequest pageRequest = new PageRequest(1, 10);
            pageRequest.setStartDate("2024-01-01");
            pageRequest.setEndDate("2024-12-31");

            when(entityManager.createQuery(anyString(), eq(RekapPenjualanDto.class))).thenReturn(typedQuery);
            when(entityManager.createQuery(anyString())).thenReturn(jpaQuery);
            when(jpaQuery.getSingleResult()).thenReturn(2L);
            when(typedQuery.getResultList()).thenReturn(List.of(new RekapPenjualanDto()));

            PageResponse<RekapPenjualanDto> result = spkReportService.findPaginatedWithPenjualan(pageRequest);

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should handle ascending sort")
        void testFindPaginatedWithPenjualan_ascendingSort() {
            PageRequest pageRequest = new PageRequest(1, 10);
            pageRequest.setSortBy("statusSpk");
            pageRequest.setDescending(false);

            when(entityManager.createQuery(contains("order by s.statusSpk asc"), eq(RekapPenjualanDto.class))).thenReturn(typedQuery);
            when(entityManager.createQuery(contains("COUNT"))).thenReturn(jpaQuery);
            when(jpaQuery.getSingleResult()).thenReturn(0L);
            when(typedQuery.getResultList()).thenReturn(List.of());

            PageResponse<RekapPenjualanDto> result = spkReportService.findPaginatedWithPenjualan(pageRequest);

            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("findByIdWithPenjualan")
    class FindByIdWithPenjualanTests {

        @Test
        @DisplayName("Should find by ID with penjualan when found")
        void testFindByIdWithPenjualan_found() {
            RekapPenjualanDto dto = new RekapPenjualanDto();
            dto.setNoSpk("SPK001");

            when(entityManager.createQuery(anyString())).thenReturn(jpaQuery);
            when(jpaQuery.setParameter(eq(1), eq(1L))).thenReturn(jpaQuery);
            when(jpaQuery.getSingleResult()).thenReturn(dto);

            TbSpkDetailEntity detail = new TbSpkDetailEntity();
            detail.setId(new TbSpkDetailId("SPK001", "Jasa A"));
            when(detailRepository.find(eq("id.noSpk"), eq("SPK001"))).thenReturn(detailPanacheQuery);
            when(detailPanacheQuery.list()).thenReturn(List.of(detail));

            RekapPenjualanDto result = spkReportService.findByIdWithPenjualan(1L);

            assertNotNull(result);
            assertNotNull(result.getDetails());
            assertEquals(1, result.getDetails().size());
            verify(enrichmentService).enrich(dto);
        }

        @Test
        @DisplayName("Should return null when not found")
        void testFindByIdWithPenjualan_notFound() {
            when(entityManager.createQuery(anyString())).thenReturn(jpaQuery);
            when(jpaQuery.setParameter(eq(1), eq(999L))).thenReturn(jpaQuery);
            when(jpaQuery.getSingleResult()).thenReturn(null);

            RekapPenjualanDto result = spkReportService.findByIdWithPenjualan(999L);

            assertNull(result);
            verify(enrichmentService, never()).enrich(any());
        }
    }

    @Nested
    @DisplayName("findUnprocessedSpk")
    class FindUnprocessedSpkTests {

        @Test
        @DisplayName("Should find unprocessed SPK")
        void testFindUnprocessedSpk() {
            List<TbSpkEntity> unprocessedList = List.of(testSpkEntity);
            when(repository.find(anyString())).thenReturn(panacheQuery);
            when(panacheQuery.list()).thenReturn(unprocessedList);

            List<TbSpkEntity> result = spkReportService.findUnprocessedSpk();

            assertNotNull(result);
            assertEquals(1, result.size());
            verify(enrichmentService).fillRequiredFields(unprocessedList);
        }

        @Test
        @DisplayName("Should return empty list when no unprocessed SPK")
        void testFindUnprocessedSpk_empty() {
            when(repository.find(anyString())).thenReturn(panacheQuery);
            when(panacheQuery.list()).thenReturn(Collections.emptyList());

            List<TbSpkEntity> result = spkReportService.findUnprocessedSpk();

            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(enrichmentService).fillRequiredFields(Collections.emptyList());
        }
    }

    @Nested
    @DisplayName("findByNoSpk")
    class FindByNoSpkTests {

        @Test
        @DisplayName("Should find SPK by noSpk when found")
        void testFindByNoSpk_found() {
            when(repository.find(eq("noSpk"), eq("SPK001"))).thenReturn(panacheQuery);
            when(panacheQuery.firstResult()).thenReturn(testSpkEntity);

            TbSpkEntity result = spkReportService.findByNoSpk("SPK001");

            assertNotNull(result);
            verify(enrichmentService).fillRequiredFields(List.of(testSpkEntity));
        }

        @Test
        @DisplayName("Should return null when SPK not found")
        void testFindByNoSpk_notFound() {
            when(repository.find(eq("noSpk"), eq("NONEXISTENT"))).thenReturn(panacheQuery);
            when(panacheQuery.firstResult()).thenReturn(null);

            TbSpkEntity result = spkReportService.findByNoSpk("NONEXISTENT");

            assertNull(result);
            verify(enrichmentService, never()).fillRequiredFields(anyList());
        }
    }
}
