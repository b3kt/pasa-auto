package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.application.dto.pazaauto.SummaryDto;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.SummaryReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SummaryService Tests")
class SummaryServiceTest {

    @Mock
    SummaryReportRepository repo;

    @InjectMocks
    SummaryService summaryService;

    private Date startDate;
    private Date endDate;

    @BeforeEach
    void setUp() {
        startDate = Date.valueOf(LocalDate.of(2024, 1, 1));
        endDate = Date.valueOf(LocalDate.of(2024, 1, 31));
    }

    private void stubEmptyQueries() {
        when(repo.countDistinctSpk(any(Date.class), any(Date.class))).thenReturn(0L);
        when(repo.sumPenjualanGrandTotal(any(Date.class), any(Date.class))).thenReturn(BigDecimal.ZERO);
        when(repo.sumPembelianGrandTotal(any(Date.class), any(Date.class), anyList())).thenReturn(BigDecimal.ZERO);
        when(repo.sumItemTerjual(any(Date.class), any(Date.class))).thenReturn(0L);
        when(repo.findDailyIncome(any(Date.class), any(Date.class))).thenReturn(List.of());
        when(repo.findDailyOutcome(any(Date.class), any(Date.class), anyList())).thenReturn(List.of());
        when(repo.findSoldItemsPerDay(any(Date.class), any(Date.class))).thenReturn(List.of());
        when(repo.findJasaSummaryPerDay(any(Date.class), any(Date.class))).thenReturn(List.of());
        when(repo.findTopItems(any(Date.class), any(Date.class))).thenReturn(List.of());
        when(repo.findIncomeByMethod(any(Date.class), any(Date.class))).thenReturn(List.of());
        when(repo.findOutcomeByType(any(Date.class), any(Date.class), anyList())).thenReturn(List.of());
        when(repo.findMekanikSummary(any(Date.class), any(Date.class))).thenReturn(List.of());
        when(repo.findMekanikPerDay(any(Date.class), any(Date.class))).thenReturn(List.of());
    }

    @Nested
    @DisplayName("getSummary")
    class GetSummaryTests {

        @Test
        @DisplayName("Should return populated DTO with all sections")
        void getSummary() {
            when(repo.countDistinctSpk(any(Date.class), any(Date.class))).thenReturn(10L);
            when(repo.sumPenjualanGrandTotal(any(Date.class), any(Date.class)))
                    .thenReturn(BigDecimal.valueOf(1000000));
            when(repo.sumPembelianGrandTotal(any(Date.class), any(Date.class), anyList()))
                    .thenReturn(BigDecimal.valueOf(500000));
            when(repo.sumItemTerjual(any(Date.class), any(Date.class))).thenReturn(50L);
            when(repo.findDailyIncome(any(Date.class), any(Date.class))).thenReturn(List.of());
            when(repo.findDailyOutcome(any(Date.class), any(Date.class), anyList())).thenReturn(List.of());
            when(repo.findSoldItemsPerDay(any(Date.class), any(Date.class))).thenReturn(List.of());
            when(repo.findJasaSummaryPerDay(any(Date.class), any(Date.class))).thenReturn(List.of());
            when(repo.findTopItems(any(Date.class), any(Date.class))).thenReturn(List.of());
            when(repo.findIncomeByMethod(any(Date.class), any(Date.class))).thenReturn(List.of());
            when(repo.findOutcomeByType(any(Date.class), any(Date.class), anyList())).thenReturn(List.of());
            when(repo.findMekanikSummary(any(Date.class), any(Date.class))).thenReturn(List.of());
            when(repo.findMekanikPerDay(any(Date.class), any(Date.class))).thenReturn(List.of());

            SummaryDto dto = summaryService.getSummary("2024-01-01", "2024-01-31", null);

            assertNotNull(dto);
            assertEquals(10L, dto.getTotalCustomers());
            assertEquals(BigDecimal.valueOf(1000000), dto.getTotalIncome());
            assertEquals(BigDecimal.valueOf(500000), dto.getTotalOutcome());
            assertEquals(BigDecimal.valueOf(500000), dto.getNetProfit());
            assertEquals(50L, dto.getTotalItemTerjual());
        }

        @Test
        @DisplayName("Should parse status filter with multiple values")
        void getSummaryWithStatusFilter() {
            stubEmptyQueries();
            SummaryDto dto = summaryService.getSummary("2024-01-01", "2024-01-31", "LUNAS,DP");
            assertNotNull(dto);
        }

        @Test
        @DisplayName("Should parse status filter with spaces")
        void getSummaryWithSpaces() {
            stubEmptyQueries();
            SummaryDto dto = summaryService.getSummary("2024-01-01", "2024-01-31", " LUNAS , DP ");
            assertNotNull(dto);
        }

        @Test
        @DisplayName("Should handle empty status filter")
        void getSummaryEmptyStatusFilter() {
            stubEmptyQueries();
            SummaryDto dto = summaryService.getSummary("2024-01-01", "2024-01-31", "");
            assertNotNull(dto);
        }

        @Test
        @DisplayName("Should handle blank status filter")
        void getSummaryBlankStatusFilter() {
            stubEmptyQueries();
            SummaryDto dto = summaryService.getSummary("2024-01-01", "2024-01-31", "   ");
            assertNotNull(dto);
        }
    }

    @Nested
    @DisplayName("buildDailyBreakdown")
    class DailyBreakdownTests {

        @Test
        @DisplayName("Should map daily income and outcome with matching dates")
        void dailyBreakdown_matchingDates() {
            stubEmptyQueries();

            Object[] incomeRow = {"2024-01-15", 5L, BigDecimal.valueOf(200000), 10L};
            when(repo.findDailyIncome(any(Date.class), any(Date.class)))
                    .thenReturn(List.<Object[]>of(incomeRow));

            Object[] outcomeRow = {"2024-01-15", BigDecimal.valueOf(100000)};
            when(repo.findDailyOutcome(any(Date.class), any(Date.class), anyList()))
                    .thenReturn(List.<Object[]>of(outcomeRow));

            SummaryDto dto = summaryService.getSummary("2024-01-01", "2024-01-31", null);

            assertNotNull(dto.getDailyBreakdown());
            assertEquals(1, dto.getDailyBreakdown().size());
            assertEquals("2024-01-15", dto.getDailyBreakdown().get(0).getDate());
            assertEquals(5L, dto.getDailyBreakdown().get(0).getCustomers());
            assertEquals(BigDecimal.valueOf(200000), dto.getDailyBreakdown().get(0).getIncome());
            assertEquals(BigDecimal.valueOf(100000), dto.getDailyBreakdown().get(0).getOutcome());
            assertEquals(BigDecimal.valueOf(100000), dto.getDailyBreakdown().get(0).getNet());
        }

        @Test
        @DisplayName("Should default outcome to ZERO when no matching date")
        void dailyBreakdown_noOutcome() {
            stubEmptyQueries();

            Object[] incomeRow = {"2024-01-15", 5L, BigDecimal.valueOf(200000), 10L};
            when(repo.findDailyIncome(any(Date.class), any(Date.class)))
                    .thenReturn(List.<Object[]>of(incomeRow));
            when(repo.findDailyOutcome(any(Date.class), any(Date.class), anyList()))
                    .thenReturn(List.of());

            SummaryDto dto = summaryService.getSummary("2024-01-01", "2024-01-31", null);

            assertEquals(BigDecimal.ZERO, dto.getDailyBreakdown().get(0).getOutcome());
            assertEquals(BigDecimal.valueOf(200000), dto.getDailyBreakdown().get(0).getNet());
        }

        @Test
        @DisplayName("Should handle multiple days")
        void dailyBreakdown_multipleDays() {
            stubEmptyQueries();

            Object[] row1 = {"2024-01-15", 5L, BigDecimal.valueOf(200000), 10L};
            Object[] row2 = {"2024-01-16", 3L, BigDecimal.valueOf(150000), 5L};
            when(repo.findDailyIncome(any(Date.class), any(Date.class)))
                    .thenReturn(List.<Object[]>of(row1, row2));
            when(repo.findDailyOutcome(any(Date.class), any(Date.class), anyList()))
                    .thenReturn(List.of());

            SummaryDto dto = summaryService.getSummary("2024-01-01", "2024-01-31", null);

            assertEquals(2, dto.getDailyBreakdown().size());
        }
    }

    @Nested
    @DisplayName("mapSoldItems")
    class SoldItemsTests {

        @Test
        @DisplayName("Should map sold items with null values")
        void soldItemsNullValues() {
            stubEmptyQueries();

            Object[] row = {"2024-01-15", null, null, 5L, BigDecimal.valueOf(100000), BigDecimal.ZERO, null};
            when(repo.findSoldItemsPerDay(any(Date.class), any(Date.class))).thenReturn(List.<Object[]>of(row));

            SummaryDto dto = summaryService.getSummary("2024-01-01", "2024-01-31", null);

            assertEquals(1, dto.getSoldItemsBreakdown().size());
            assertNull(dto.getSoldItemsBreakdown().get(0).getSparepartId());
            assertEquals(BigDecimal.ZERO, dto.getSoldItemsBreakdown().get(0).getTotalModal());
        }

        @Test
        @DisplayName("Should map sold items with non-null values")
        void soldItemsNonNullValues() {
            stubEmptyQueries();

            Object[] row = {"2024-01-15", 1L, "Sparepart A", 5L,
                    BigDecimal.valueOf(100000), BigDecimal.valueOf(5000),
                    BigDecimal.valueOf(75000)};
            when(repo.findSoldItemsPerDay(any(Date.class), any(Date.class))).thenReturn(List.<Object[]>of(row));

            SummaryDto dto = summaryService.getSummary("2024-01-01", "2024-01-31", null);

            assertEquals(1L, dto.getSoldItemsBreakdown().get(0).getSparepartId());
            assertEquals("Sparepart A", dto.getSoldItemsBreakdown().get(0).getNamaBarang());
        }
    }

    @Nested
    @DisplayName("mapJasaSummary")
    class JasaSummaryTests {

        @Test
        @DisplayName("Should map jasa summary with null values")
        void jasaSummaryNullValues() {
            stubEmptyQueries();

            Object[] row = {"2024-01-15", null, null, 3L,
                    BigDecimal.valueOf(50000), BigDecimal.ZERO, null};
            when(repo.findJasaSummaryPerDay(any(Date.class), any(Date.class)))
                    .thenReturn(List.<Object[]>of(row));

            SummaryDto dto = summaryService.getSummary("2024-01-01", "2024-01-31", null);

            assertEquals(1, dto.getJasaSummaryBreakdown().size());
            assertNull(dto.getJasaSummaryBreakdown().get(0).getJasaId());
            assertEquals(BigDecimal.ZERO, dto.getJasaSummaryBreakdown().get(0).getTotalModal());
        }

        @Test
        @DisplayName("Should map jasa summary with non-null values")
        void jasaSummaryNonNullValues() {
            stubEmptyQueries();

            Object[] row = {"2024-01-15", 1L, "Service A", 3L,
                    BigDecimal.valueOf(50000), BigDecimal.valueOf(2000),
                    BigDecimal.valueOf(30000)};
            when(repo.findJasaSummaryPerDay(any(Date.class), any(Date.class)))
                    .thenReturn(List.<Object[]>of(row));

            SummaryDto dto = summaryService.getSummary("2024-01-01", "2024-01-31", null);

            assertEquals(1L, dto.getJasaSummaryBreakdown().get(0).getJasaId());
            assertEquals("Service A", dto.getJasaSummaryBreakdown().get(0).getNamaJasa());
        }
    }

    @Nested
    @DisplayName("mapTopItems")
    class TopItemsTests {

        @Test
        @DisplayName("Should map top items with null values")
        void topItemsNullValues() {
            stubEmptyQueries();

            Object[] row = {null, null, 10L, BigDecimal.valueOf(500000)};
            when(repo.findTopItems(any(Date.class), any(Date.class)))
                    .thenReturn(List.<Object[]>of(row));

            SummaryDto dto = summaryService.getSummary("2024-01-01", "2024-01-31", null);

            assertEquals(1, dto.getTopItems().size());
            assertNull(dto.getTopItems().get(0).getSparepartId());
        }

        @Test
        @DisplayName("Should map top items with non-null values")
        void topItemsNonNullValues() {
            stubEmptyQueries();

            Object[] row = {1L, "Sparepart A", 10L, BigDecimal.valueOf(500000)};
            when(repo.findTopItems(any(Date.class), any(Date.class)))
                    .thenReturn(List.<Object[]>of(row));

            SummaryDto dto = summaryService.getSummary("2024-01-01", "2024-01-31", null);

            assertEquals(1L, dto.getTopItems().get(0).getSparepartId());
        }
    }

    @Nested
    @DisplayName("mapIncomeByMethod")
    class IncomeByMethodTests {

        @Test
        @DisplayName("Should map income by method")
        void incomeByMethod() {
            stubEmptyQueries();

            Object[] row = {"CASH", BigDecimal.valueOf(500000)};
            when(repo.findIncomeByMethod(any(Date.class), any(Date.class)))
                    .thenReturn(List.<Object[]>of(row));

            SummaryDto dto = summaryService.getSummary("2024-01-01", "2024-01-31", null);

            assertEquals(1, dto.getIncomeByMethod().size());
            assertEquals("CASH", dto.getIncomeByMethod().get(0).getLabel());
            assertEquals(BigDecimal.valueOf(500000), dto.getIncomeByMethod().get(0).getAmount());
        }
    }

    @Nested
    @DisplayName("mapOutcomeByType")
    class OutcomeByTypeTests {

        @Test
        @DisplayName("Should map outcome by type")
        void outcomeByType() {
            stubEmptyQueries();

            Object[] row = {"SPAREPART", BigDecimal.valueOf(300000)};
            when(repo.findOutcomeByType(any(Date.class), any(Date.class), anyList()))
                    .thenReturn(List.<Object[]>of(row));

            SummaryDto dto = summaryService.getSummary("2024-01-01", "2024-01-31", null);

            assertEquals(1, dto.getOutcomeByType().size());
            assertEquals("SPAREPART", dto.getOutcomeByType().get(0).getLabel());
            assertEquals(BigDecimal.valueOf(300000), dto.getOutcomeByType().get(0).getAmount());
        }
    }

    @Nested
    @DisplayName("mapMekanikSummary")
    class MekanikSummaryTests {

        @Test
        @DisplayName("Should calculate rataPerHari correctly")
        void mekanikSummaryWithRata() {
            stubEmptyQueries();

            Object[] mekanikRow = {1L, "Budi", 10L, 5L};
            when(repo.findMekanikSummary(any(Date.class), any(Date.class)))
                    .thenReturn(List.<Object[]>of(mekanikRow));

            SummaryDto dto = summaryService.getSummary("2024-01-01", "2024-01-31", null);

            assertEquals(1, dto.getMekanikSummary().size());
            assertEquals(1L, dto.getMekanikSummary().get(0).getMekanikId());
            assertEquals("Budi", dto.getMekanikSummary().get(0).getNamaMekanik());
            assertEquals(10L, dto.getMekanikSummary().get(0).getTotalCustomers());
            assertEquals(2.0, dto.getMekanikSummary().get(0).getRataPerHari(), 0.1);
        }

        @Test
        @DisplayName("Should handle zero totalHari")
        void mekanikSummaryZeroDays() {
            stubEmptyQueries();

            Object[] mekanikRow = {1L, "Budi", 0L, 0L};
            when(repo.findMekanikSummary(any(Date.class), any(Date.class)))
                    .thenReturn(List.<Object[]>of(mekanikRow));

            SummaryDto dto = summaryService.getSummary("2024-01-01", "2024-01-31", null);

            assertEquals(0, dto.getMekanikSummary().get(0).getRataPerHari(), 0);
        }
    }

    @Nested
    @DisplayName("mapMekanikPerDay")
    class MekanikPerDayTests {

        @Test
        @DisplayName("Should map mekanik per day")
        void mekanikPerDay() {
            stubEmptyQueries();

            Object[] row = {"2024-01-15", 1L, "Budi", 3L};
            when(repo.findMekanikPerDay(any(Date.class), any(Date.class)))
                    .thenReturn(List.<Object[]>of(row));

            SummaryDto dto = summaryService.getSummary("2024-01-01", "2024-01-31", null);

            assertEquals(1, dto.getMekanikBreakdown().size());
            assertEquals("2024-01-15", dto.getMekanikBreakdown().get(0).getDate());
            assertEquals(1L, dto.getMekanikBreakdown().get(0).getMekanikId());
            assertEquals("Budi", dto.getMekanikBreakdown().get(0).getNamaMekanik());
            assertEquals(3L, dto.getMekanikBreakdown().get(0).getTotalCustomers());
        }
    }
}
