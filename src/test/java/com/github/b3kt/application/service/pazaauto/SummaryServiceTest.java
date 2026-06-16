package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.application.dto.pazaauto.SummaryDto;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.SummaryReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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

    @Test
    @DisplayName("getSummary returns populated DTO with all sections")
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
    @DisplayName("getSummary with status filter")
    void getSummaryWithStatusFilter() {
        when(repo.countDistinctSpk(any(Date.class), any(Date.class))).thenReturn(0L);
        when(repo.sumPenjualanGrandTotal(any(Date.class), any(Date.class)))
                .thenReturn(BigDecimal.ZERO);
        when(repo.sumPembelianGrandTotal(any(Date.class), any(Date.class), anyList()))
                .thenReturn(BigDecimal.ZERO);
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

        SummaryDto dto = summaryService.getSummary("2024-01-01", "2024-01-31", "LUNAS,DP");
        assertNotNull(dto);
    }

    @Test
    @DisplayName("buildDailyBreakdown maps daily income and outcome")
    void dailyBreakdown() {
        when(repo.countDistinctSpk(any(Date.class), any(Date.class))).thenReturn(0L);
        when(repo.sumPenjualanGrandTotal(any(Date.class), any(Date.class))).thenReturn(BigDecimal.ZERO);
        when(repo.sumPembelianGrandTotal(any(Date.class), any(Date.class), anyList())).thenReturn(BigDecimal.ZERO);
        when(repo.sumItemTerjual(any(Date.class), any(Date.class))).thenReturn(0L);
        when(repo.findTopItems(any(Date.class), any(Date.class))).thenReturn(List.of());
        when(repo.findIncomeByMethod(any(Date.class), any(Date.class))).thenReturn(List.of());
        when(repo.findOutcomeByType(any(Date.class), any(Date.class), anyList())).thenReturn(List.of());
        when(repo.findMekanikSummary(any(Date.class), any(Date.class))).thenReturn(List.of());
        when(repo.findMekanikPerDay(any(Date.class), any(Date.class))).thenReturn(List.of());
        when(repo.findSoldItemsPerDay(any(Date.class), any(Date.class))).thenReturn(List.of());
        when(repo.findJasaSummaryPerDay(any(Date.class), any(Date.class))).thenReturn(List.of());

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
    @DisplayName("mapMekanikSummary calculates rataPerHari correctly")
    void mekanikSummaryWithRata() {
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
        when(repo.findMekanikPerDay(any(Date.class), any(Date.class))).thenReturn(List.of());

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
    @DisplayName("mapMekanikSummary handles zero totalHari")
    void mekanikSummaryZeroDays() {
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
        when(repo.findMekanikPerDay(any(Date.class), any(Date.class))).thenReturn(List.of());

        Object[] mekanikRow = {1L, "Budi", 0L, 0L};
        when(repo.findMekanikSummary(any(Date.class), any(Date.class)))
                .thenReturn(List.<Object[]>of(mekanikRow));

        SummaryDto dto = summaryService.getSummary("2024-01-01", "2024-01-31", null);

        assertEquals(0, dto.getMekanikSummary().get(0).getRataPerHari(), 0);
    }

    @Test
    @DisplayName("mapSoldItems handles null values")
    void soldItemsNullValues() {
        when(repo.countDistinctSpk(any(Date.class), any(Date.class))).thenReturn(0L);
        when(repo.sumPenjualanGrandTotal(any(Date.class), any(Date.class))).thenReturn(BigDecimal.ZERO);
        when(repo.sumPembelianGrandTotal(any(Date.class), any(Date.class), anyList())).thenReturn(BigDecimal.ZERO);
        when(repo.sumItemTerjual(any(Date.class), any(Date.class))).thenReturn(0L);
        when(repo.findDailyIncome(any(Date.class), any(Date.class))).thenReturn(List.of());
        when(repo.findDailyOutcome(any(Date.class), any(Date.class), anyList())).thenReturn(List.of());
        when(repo.findTopItems(any(Date.class), any(Date.class))).thenReturn(List.of());
        when(repo.findIncomeByMethod(any(Date.class), any(Date.class))).thenReturn(List.of());
        when(repo.findOutcomeByType(any(Date.class), any(Date.class), anyList())).thenReturn(List.of());
        when(repo.findMekanikSummary(any(Date.class), any(Date.class))).thenReturn(List.of());
        when(repo.findMekanikPerDay(any(Date.class), any(Date.class))).thenReturn(List.of());

        Object[] row = {"2024-01-15", null, null, 5L, BigDecimal.valueOf(100000), BigDecimal.ZERO, null};
        when(repo.findSoldItemsPerDay(any(Date.class), any(Date.class))).thenReturn(List.<Object[]>of(row));

        SummaryDto dto = summaryService.getSummary("2024-01-01", "2024-01-31", null);
        assertEquals(1, dto.getSoldItemsBreakdown().size());
        assertNull(dto.getSoldItemsBreakdown().get(0).getSparepartId());
        assertEquals(BigDecimal.ZERO, dto.getSoldItemsBreakdown().get(0).getTotalModal());
    }
}
