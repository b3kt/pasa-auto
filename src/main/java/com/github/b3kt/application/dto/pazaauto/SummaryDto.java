package com.github.b3kt.application.dto.pazaauto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@RegisterForReflection
public class SummaryDto {

    private long totalCustomers;
    private BigDecimal totalIncome = BigDecimal.ZERO;
    private BigDecimal totalOutcome = BigDecimal.ZERO;
    private BigDecimal netProfit = BigDecimal.ZERO;
    private long totalItemTerjual;

    private List<DailyBreakdownDto> dailyBreakdown = new ArrayList<>();
    private List<SoldItemDailyDto> soldItemsBreakdown = new ArrayList<>();
    private List<JasaSummaryDailyDto> jasaSummaryBreakdown = new ArrayList<>();
    private List<TopItemDto> topItems = new ArrayList<>();
    private List<IncomeByMethodDto> incomeByMethod = new ArrayList<>();
    private List<OutcomeByTypeDto> outcomeByType = new ArrayList<>();
    private List<MekanikSummaryDto> mekanikSummary = new ArrayList<>();
    private List<MekanikDailyDto> mekanikBreakdown = new ArrayList<>();

    @Data
    @RegisterForReflection
    public static class DailyBreakdownDto {
        private String date;
        private long customers;
        private BigDecimal income = BigDecimal.ZERO;
        private BigDecimal outcome = BigDecimal.ZERO;
        private BigDecimal net = BigDecimal.ZERO;
        private long itemsTerjual;
    }

    @Data
    @RegisterForReflection
    public static class SoldItemDailyDto {
        private String date;
        private Long sparepartId;
        private String namaBarang;
        private long totalQty;
        private BigDecimal totalValue = BigDecimal.ZERO;
        private BigDecimal totalNilaiAdjustment = BigDecimal.ZERO;
        private BigDecimal totalModal = BigDecimal.ZERO;
    }

    @Data
    @RegisterForReflection
    public static class JasaSummaryDailyDto {
        private String date;
        private Long jasaId;
        private String namaJasa;
        private long totalQty;
        private BigDecimal totalNilai = BigDecimal.ZERO;
        private BigDecimal totalNilaiAdjustment = BigDecimal.ZERO;
        private BigDecimal totalModal = BigDecimal.ZERO;
    }

    @Data
    @RegisterForReflection
    public static class TopItemDto {
        private Long sparepartId;
        private String namaBarang;
        private long totalQty;
        private BigDecimal totalValue = BigDecimal.ZERO;
    }

    @Data
    @RegisterForReflection
    public static class IncomeByMethodDto {
        private String label;
        private BigDecimal amount = BigDecimal.ZERO;
    }

    @Data
    @RegisterForReflection
    public static class OutcomeByTypeDto {
        private String label;
        private BigDecimal amount = BigDecimal.ZERO;
    }

    @Data
    @RegisterForReflection
    public static class MekanikSummaryDto {
        private Long mekanikId;
        private String namaMekanik;
        private long totalCustomers;
        private long totalHari;
        private double rataPerHari;
    }

    @Data
    @RegisterForReflection
    public static class MekanikDailyDto {
        private String date;
        private Long mekanikId;
        private String namaMekanik;
        private long totalCustomers;
    }
}
