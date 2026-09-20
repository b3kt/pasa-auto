package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.application.dto.pazaauto.RekapPenjualanDto;
import com.github.b3kt.application.dto.pazaauto.RekapPenjualanSummaryDto;
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
import org.mockito.ArgumentCaptor;
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

    /**
     * Totals shown above the rekap penjualan table. The query aggregates over a LEFT JOIN, so the
     * driver hands back a row of nullable Numbers of assorted types - the mapping has to survive
     * each of them.
     */
    @Nested
    @DisplayName("summarizeWithPenjualan")
    class SummarizeWithPenjualanTests {

        private Object[] row(Object... values) {
            return values;
        }

        private void stubSummaryRow(Object[] row) {
            when(entityManager.createQuery(anyString())).thenReturn(jpaQuery);
            when(jpaQuery.getSingleResult()).thenReturn(row);
        }

        @Test
        @DisplayName("Should map every aggregate onto the summary")
        void testSummarize_mapsAllColumns() {
            stubSummaryRow(row(12L, 7L, 5L, new java.math.BigDecimal("1500000.50"), 3600.0d, 9L));

            RekapPenjualanSummaryDto summary = spkReportService.summarizeWithPenjualan(new PageRequest(1, 10));

            assertEquals(12L, summary.getTotalSpk());
            assertEquals(7L, summary.getTotalPelanggan());
            assertEquals(5L, summary.getTotalKendaraan());
            assertEquals(0, new java.math.BigDecimal("1500000.50").compareTo(summary.getTotalDibayar()));
            assertEquals(3600L, summary.getAvgCompletionSeconds());
            assertEquals(9L, summary.getCompletedCount());
        }

        /** No penjualan rows joined means a null sum, which must read as zero and not as null. */
        @Test
        @DisplayName("Should report a missing payment total as zero")
        void testSummarize_nullTotalBecomesZero() {
            stubSummaryRow(row(0L, 0L, 0L, null, null, 0L));

            RekapPenjualanSummaryDto summary = spkReportService.summarizeWithPenjualan(new PageRequest(1, 10));

            assertEquals(java.math.BigDecimal.ZERO, summary.getTotalDibayar());
        }

        /**
         * avg() over rows that never completed is null. That has to stay null rather than becoming
         * zero, which would read as "completed instantly" in the report.
         */
        @Test
        @DisplayName("Should leave an unknown average completion time null")
        void testSummarize_nullAverageStaysNull() {
            stubSummaryRow(row(4L, 4L, 4L, java.math.BigDecimal.TEN, null, 0L));

            assertNull(spkReportService.summarizeWithPenjualan(new PageRequest(1, 10)).getAvgCompletionSeconds());
        }

        /** count() and avg() come back as assorted Number subtypes depending on the driver. */
        @Test
        @DisplayName("Should accept any Number type from the driver")
        void testSummarize_acceptsAssortedNumberTypes() {
            stubSummaryRow(row(java.math.BigInteger.valueOf(3), 2, (short) 1, 250L, java.math.BigDecimal.valueOf(90.7), 1));

            RekapPenjualanSummaryDto summary = spkReportService.summarizeWithPenjualan(new PageRequest(1, 10));

            assertEquals(3L, summary.getTotalSpk());
            assertEquals(2L, summary.getTotalPelanggan());
            assertEquals(1L, summary.getTotalKendaraan());
            assertEquals(0, new java.math.BigDecimal("250").compareTo(summary.getTotalDibayar()));
            assertEquals(90L, summary.getAvgCompletionSeconds());
            assertEquals(1L, summary.getCompletedCount());
        }

        /**
         * Every filter field is aliased, because noSpk and namaPelanggan exist on both joined
         * entities and Hibernate rejects them unqualified.
         */
        @Test
        @DisplayName("Should qualify every filter with its table alias")
        void testSummarize_filtersAreAliased() {
            PageRequest pageRequest = new PageRequest(1, 10);
            pageRequest.setSearch("budi");
            pageRequest.setStatusFilter("SELESAI");
            pageRequest.setStatusPembayaranFilter("LUNAS");
            pageRequest.setStartDate("2024-01-01");
            pageRequest.setEndDate("2024-12-31");
            stubSummaryRow(row(1L, 1L, 1L, java.math.BigDecimal.ONE, 1.0d, 1L));

            spkReportService.summarizeWithPenjualan(pageRequest);

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(entityManager).createQuery(captor.capture());
            String jpql = captor.getValue();

            assertTrue(jpql.contains("s.noSpk"), jpql);
            assertTrue(jpql.contains("s.statusSpk"), jpql);
            assertTrue(jpql.contains("p.statusPembayaran"), jpql);
            assertTrue(jpql.contains("s.tanggalJamSpk"), jpql);
            // The base query's WHERE 1=1 is the anchor; the builder's own 1=1 is stripped so the
            // filters append cleanly rather than producing "1=1 1=1 and ...".
            assertEquals(1, jpql.split("1=1", -1).length - 1, "only the anchor 1=1 survives: " + jpql);
        }

        @Test
        @DisplayName("Should bind every filter parameter positionally")
        void testSummarize_bindsParameters() {
            PageRequest pageRequest = new PageRequest(1, 10);
            pageRequest.setSearch("budi");
            stubSummaryRow(row(1L, 1L, 1L, java.math.BigDecimal.ONE, 1.0d, 1L));

            spkReportService.summarizeWithPenjualan(pageRequest);

            verify(jpaQuery, atLeastOnce()).setParameter(eq(1), any());
        }

        @Test
        @DisplayName("Should run without filters")
        void testSummarize_noFilters() {
            stubSummaryRow(row(0L, 0L, 0L, null, null, 0L));

            assertNotNull(spkReportService.summarizeWithPenjualan(new PageRequest(1, 10)));
            verify(jpaQuery, never()).setParameter(anyInt(), any());
        }
    }

    /**
     * noSpk and namaPelanggan exist on both joined entities, so the sort field decides the alias:
     * a penjualan column sorts on p., everything else on s. Getting this wrong is an ambiguous
     * attribute reference at runtime, not a compile error.
     */
    @Nested
    @DisplayName("rekap sort aliasing")
    class RekapSortAliasTests {

        private String orderByFor(String sortField, boolean descending) {
            PageRequest pageRequest = new PageRequest(1, 10);
            pageRequest.setSortBy(sortField);
            pageRequest.setDescending(descending);

            when(entityManager.createQuery(anyString(), eq(RekapPenjualanDto.class))).thenReturn(typedQuery);
            when(entityManager.createQuery(anyString())).thenReturn(jpaQuery);
            when(jpaQuery.getSingleResult()).thenReturn(0L);
            when(typedQuery.getResultList()).thenReturn(List.of());

            spkReportService.findPaginatedWithPenjualan(pageRequest);

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(entityManager, atLeastOnce()).createQuery(captor.capture(), eq(RekapPenjualanDto.class));
            String jpql = captor.getValue();
            return jpql.substring(jpql.indexOf(" order by "));
        }

        @Test
        @DisplayName("a penjualan column sorts on the penjualan alias")
        void penjualanColumnsUsePenjualanAlias() {
            assertEquals(" order by p.grandTotal desc", orderByFor("grandTotal", true));
            assertEquals(" order by p.statusPembayaran asc", orderByFor("statusPembayaran", false));
            assertEquals(" order by p.noPenjualan asc", orderByFor("noPenjualan", false));
        }

        @Test
        @DisplayName("any other column sorts on the SPK alias")
        void otherColumnsUseSpkAlias() {
            assertEquals(" order by s.noSpk asc", orderByFor("noSpk", false));
            assertEquals(" order by s.nopol desc", orderByFor("nopol", true));
        }

        @Test
        @DisplayName("the sorted query is re-bound with the same parameters")
        void sortedQueryRebindsParameters() {
            PageRequest pageRequest = new PageRequest(1, 10);
            pageRequest.setSearch("budi");
            pageRequest.setSortBy("grandTotal");

            when(entityManager.createQuery(anyString(), eq(RekapPenjualanDto.class))).thenReturn(typedQuery);
            when(entityManager.createQuery(anyString())).thenReturn(jpaQuery);
            when(jpaQuery.getSingleResult()).thenReturn(0L);
            when(typedQuery.getResultList()).thenReturn(List.of());

            spkReportService.findPaginatedWithPenjualan(pageRequest);

            // once for the unsorted query, once for the sorted rebuild
            verify(typedQuery, atLeast(2)).setParameter(eq(1), any());
        }

        /** An empty sortBy is not a sort, so no order by is appended at all. */
        @Test
        @DisplayName("an empty sort field appends no order by")
        void emptySortAppendsNothing() {
            PageRequest pageRequest = new PageRequest(1, 10);
            pageRequest.setSortBy("");

            when(entityManager.createQuery(anyString(), eq(RekapPenjualanDto.class))).thenReturn(typedQuery);
            when(entityManager.createQuery(anyString())).thenReturn(jpaQuery);
            when(jpaQuery.getSingleResult()).thenReturn(0L);
            when(typedQuery.getResultList()).thenReturn(List.of());

            spkReportService.findPaginatedWithPenjualan(pageRequest);

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(entityManager).createQuery(captor.capture(), eq(RekapPenjualanDto.class));
            assertFalse(captor.getValue().contains("order by"), captor.getValue());
        }
    }
}
