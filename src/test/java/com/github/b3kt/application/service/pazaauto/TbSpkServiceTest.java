package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.application.dto.pazaauto.RekapPenjualanDto;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSpkDetailEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSpkEntity;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbSpkDetailRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbSpkRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import jakarta.persistence.EntityManager;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TbSpkService Tests")
class TbSpkServiceTest {

    @Mock
    private TbSpkRepository repository;

    @Mock
    private TbSpkDetailRepository detailRepository;

    @Mock
    private SpkDetailService spkDetailService;

    @Mock
    private SpkEnrichmentService enrichmentService;

    @Mock
    private SpkReportService reportService;

    @Mock
    private SpkNumberService numberService;

    @Mock
    private PanacheQuery<TbSpkEntity> panacheQuery;

    @Mock
    private EntityManager entityManager;

    private TbSpkService spkService;

    private TbSpkEntity testSpkEntity;

    @BeforeEach
    void setUp() {
        spkService = new TbSpkService(
                repository,
                detailRepository,
                spkDetailService,
                enrichmentService,
                reportService,
                numberService
        );

        testSpkEntity = new TbSpkEntity();
        testSpkEntity.setId(1L);
        testSpkEntity.setNoSpk("SPK202401001");
        testSpkEntity.setNopol("B1234XYZ");
        testSpkEntity.setStatusSpk("MENUNGGU");
    }

    @Nested
    @DisplayName("create")
    class CreateTests {

        @Test
        @DisplayName("Should create SPK and save details")
        void testCreate() {
            doNothing().when(repository).persist(any(TbSpkEntity.class));

            TbSpkEntity result = spkService.create(testSpkEntity);

            assertNotNull(result);
            verify(spkDetailService).saveDetails(testSpkEntity);
        }

        @Test
        @DisplayName("Should create SPK with noAntrian from noSpk suffix")
        void testCreate_setsNoAntrian() {
            testSpkEntity.setNoAntrian(null);
            testSpkEntity.setNoSpk("SPK202401005");
            doNothing().when(repository).persist(any(TbSpkEntity.class));

            TbSpkEntity result = spkService.create(testSpkEntity);

            assertEquals(5, result.getNoAntrian());
        }

        @Test
        @DisplayName("Should create SPK preserving existing noAntrian")
        void testCreate_preservesNoAntrian() {
            testSpkEntity.setNoAntrian(42);
            doNothing().when(repository).persist(any(TbSpkEntity.class));

            TbSpkEntity result = spkService.create(testSpkEntity);

            assertEquals(42, result.getNoAntrian());
        }
    }

    @Nested
    @DisplayName("update")
    class UpdateTests {

        @Test
        @DisplayName("Should update SPK, delete and save details")
        void testUpdate() {
            when(repository.findByIdOptional(1L)).thenReturn(Optional.of(testSpkEntity));
            when(repository.getEntityManager()).thenReturn(entityManager);
            when(entityManager.merge(any(TbSpkEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            TbSpkEntity result = spkService.update(1L, testSpkEntity);

            assertNotNull(result);
            verify(spkDetailService).deleteDetailsByNoSpk("SPK202401001");
            verify(spkDetailService).saveDetails(testSpkEntity);
        }

        @Test
        @DisplayName("Should set status to PROSES when startProcess is true")
        void testUpdate_startProcess() {
            when(repository.findByIdOptional(1L)).thenReturn(Optional.of(testSpkEntity));
            when(repository.getEntityManager()).thenReturn(entityManager);
            when(entityManager.merge(any(TbSpkEntity.class))).thenAnswer(inv -> {
                TbSpkEntity e = inv.getArgument(0);
                e.setStatusSpk("PROSES");
                return e;
            });

            testSpkEntity.setStartProcess(true);

            TbSpkEntity result = spkService.update(1L, testSpkEntity);

            assertEquals("PROSES", result.getStatusSpk());
            assertNotNull(result.getStartedAt());
        }

        @Test
        @DisplayName("Should not change status when startProcess is false")
        void testUpdate_noStartProcess() {
            when(repository.findByIdOptional(1L)).thenReturn(Optional.of(testSpkEntity));
            when(repository.getEntityManager()).thenReturn(entityManager);
            when(entityManager.merge(any(TbSpkEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            testSpkEntity.setStartProcess(false);

            TbSpkEntity result = spkService.update(1L, testSpkEntity);

            assertEquals("MENUNGGU", result.getStatusSpk());
        }
    }

    @Nested
    @DisplayName("findById")
    class FindByIdTests {

        @Test
        @DisplayName("Should find SPK by ID and populate details")
        void testFindById() {
            when(repository.findByIdOptional(1L)).thenReturn(Optional.of(testSpkEntity));
            when(spkDetailService.findByNoSpk("SPK202401001")).thenReturn(Collections.emptyList());

            TbSpkEntity result = spkService.findById(1L);

            assertNotNull(result);
            assertEquals("SPK202401001", result.getNoSpk());
            verify(spkDetailService).findByNoSpk("SPK202401001");
            verify(enrichmentService).enrich(testSpkEntity);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when not found")
        void testFindByIdNotFound() {
            when(repository.findByIdOptional(999L)).thenReturn(Optional.empty());

            assertThrows(jakarta.persistence.EntityNotFoundException.class,
                    () -> spkService.findById(999L));
        }

        @Test
        @DisplayName("Should populate details from spkDetailService")
        void testFindById_populatesDetails() {
            TbSpkDetailEntity detail = new TbSpkDetailEntity();
            when(repository.findByIdOptional(1L)).thenReturn(Optional.of(testSpkEntity));
            when(spkDetailService.findByNoSpk("SPK202401001")).thenReturn(List.of(detail));

            TbSpkEntity result = spkService.findById(1L);

            assertNotNull(result.getDetails());
            assertEquals(1, result.getDetails().size());
        }
    }

    @Nested
    @DisplayName("findByIdWithPenjualan")
    class FindByIdWithPenjualanTests {

        @Test
        @DisplayName("Should delegate to reportService")
        void testFindByIdWithPenjualan() {
            RekapPenjualanDto dto = new RekapPenjualanDto();
            when(reportService.findByIdWithPenjualan(1L)).thenReturn(dto);

            RekapPenjualanDto result = spkService.findByIdWithPenjualan(1L);

            assertNotNull(result);
            verify(reportService).findByIdWithPenjualan(1L);
        }
    }

    @Nested
    @DisplayName("findPaginated")
    class FindPaginatedTests {

        @Test
        @DisplayName("Should delegate to reportService.findPaginated")
        void testFindPaginated() {
            PageRequest pageRequest = new PageRequest(1, 10);
            PageResponse<TbSpkEntity> mockResponse = new PageResponse<>(List.of(testSpkEntity), 1, 10, 1);
            when(reportService.findPaginated(pageRequest)).thenReturn(mockResponse);

            PageResponse<TbSpkEntity> result = spkService.findPaginated(pageRequest);

            assertNotNull(result);
            verify(reportService).findPaginated(pageRequest);
        }

        @Test
        @DisplayName("Should handle empty results")
        void testFindPaginated_empty() {
            PageRequest pageRequest = new PageRequest(1, 10);
            PageResponse<TbSpkEntity> mockResponse = new PageResponse<>(Collections.emptyList(), 1, 10, 0);
            when(reportService.findPaginated(pageRequest)).thenReturn(mockResponse);

            PageResponse<TbSpkEntity> result = spkService.findPaginated(pageRequest);

            assertEquals(0, result.getRowsNumber());
        }
    }

    @Nested
    @DisplayName("findPaginatedWithPenjualan")
    class FindPaginatedWithPenjualanTests {

        @Test
        @DisplayName("Should delegate to reportService.findPaginatedWithPenjualan")
        void testFindPaginatedWithPenjualan() {
            PageRequest pageRequest = new PageRequest(1, 10);
            PageResponse<RekapPenjualanDto> mockResponse = new PageResponse<>(List.of(new RekapPenjualanDto()), 1, 10, 1);
            when(reportService.findPaginatedWithPenjualan(pageRequest)).thenReturn(mockResponse);

            PageResponse<RekapPenjualanDto> result = spkService.findPaginatedWithPenjualan(pageRequest);

            assertNotNull(result);
            verify(reportService).findPaginatedWithPenjualan(pageRequest);
        }
    }

    @Nested
    @DisplayName("findUnprocessedSpk")
    class FindUnprocessedSpkTests {

        @Test
        @DisplayName("Should delegate to reportService")
        void testFindUnprocessedSpk() {
            when(reportService.findUnprocessedSpk()).thenReturn(List.of(testSpkEntity));

            List<TbSpkEntity> result = spkService.findUnprocessedSpk();

            assertEquals(1, result.size());
            verify(reportService).findUnprocessedSpk();
        }
    }

    @Nested
    @DisplayName("findByNoSpk")
    class FindByNoSpkTests {

        @Test
        @DisplayName("Should delegate to reportService")
        void testFindByNoSpk() {
            when(reportService.findByNoSpk("SPK001")).thenReturn(testSpkEntity);

            TbSpkEntity result = spkService.findByNoSpk("SPK001");

            assertNotNull(result);
            verify(reportService).findByNoSpk("SPK001");
        }

        @Test
        @DisplayName("Should return null when not found")
        void testFindByNoSpkNotFound() {
            when(reportService.findByNoSpk("NONEXISTENT")).thenReturn(null);

            TbSpkEntity result = spkService.findByNoSpk("NONEXISTENT");

            assertNull(result);
        }
    }

    @Nested
    @DisplayName("getNextSpkNumber")
    class GetNextSpkNumberTests {

        @Test
        @DisplayName("Should delegate to numberService")
        void testGetNextSpkNumber() {
            when(numberService.getNextSpkNumber("SPK202401")).thenReturn("SPK20240102");

            String result = spkService.getNextSpkNumber("SPK202401");

            assertEquals("SPK20240102", result);
            verify(numberService).getNextSpkNumber("SPK202401");
        }

        @Test
        @DisplayName("Should return default number when no existing SPK")
        void testGetNextSpkNumber_default() {
            when(numberService.getNextSpkNumber("SPK202401")).thenReturn("SPK20240100");

            String result = spkService.getNextSpkNumber("SPK202401");

            assertEquals("SPK20240100", result);
        }
    }

    @Nested
    @DisplayName("generateNextSpkNumber")
    class GenerateNextSpkNumberTests {

        @Test
        @DisplayName("Should delegate to numberService")
        void testGenerateNextSpkNumber() {
            when(numberService.generateNextSpkNumber("202401")).thenReturn("SPK20240100");

            String result = spkService.generateNextSpkNumber("202401");

            assertEquals("SPK20240100", result);
            verify(numberService).generateNextSpkNumber("202401");
        }
    }

    @Nested
    @DisplayName("deleteByNoSpk")
    class DeleteByNoSpkTests {

        @Test
        @DisplayName("Should delete details and SPK by noSpk")
        void testDeleteByNoSpk() {
            spkService.deleteByNoSpk("SPK001");

            verify(spkDetailService).deleteDetailsByNoSpk("SPK001");
            verify(repository).delete("noSpk", "SPK001");
        }
    }

    @Nested
    @DisplayName("cancelSpk")
    class CancelSpkTests {

        @Test
        @DisplayName("Should cancel SPK and set BATAL status")
        void testCancelSpk() {
            when(repository.findById(1L)).thenReturn(testSpkEntity);

            TbSpkEntity result = spkService.cancelSpk(1L);

            assertNotNull(result);
            assertEquals("BATAL", result.getStatusSpk());
            assertTrue(result.getKeterangan().contains("lastStatus: MENUNGGU"));
        }

        @Test
        @DisplayName("Should return null when SPK not found")
        void testCancelSpk_notFound() {
            when(repository.findById(999L)).thenReturn(null);

            TbSpkEntity result = spkService.cancelSpk(999L);

            assertNull(result);
        }
    }

    @Nested
    @DisplayName("setEntityId")
    class SetEntityIdTests {

        @Test
        @DisplayName("Should set entity ID")
        void testSetEntityId() {
            TbSpkEntity entity = new TbSpkEntity();
            spkService.setEntityId(entity, 42L);

            assertEquals(42L, entity.getId());
        }
    }

    @Nested
    @DisplayName("getRepository")
    class GetRepositoryTests {

        @Test
        @DisplayName("Should return repository")
        void testGetRepository() {
            assertSame(repository, spkService.getRepository());
        }
    }
}
