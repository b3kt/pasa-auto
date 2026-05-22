package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.application.dto.pazaauto.SummaryDto;
import com.github.b3kt.application.dto.pazaauto.SummaryDto.DailyBreakdownDto;
import com.github.b3kt.application.dto.pazaauto.SummaryDto.TopItemDto;
import com.github.b3kt.application.dto.pazaauto.SummaryDto.SoldItemDailyDto;
import com.github.b3kt.application.dto.pazaauto.SummaryDto.JasaSummaryDailyDto;
import com.github.b3kt.application.dto.pazaauto.SummaryDto.IncomeByMethodDto;
import com.github.b3kt.application.dto.pazaauto.SummaryDto.OutcomeByTypeDto;
import com.github.b3kt.application.dto.pazaauto.SummaryDto.MekanikSummaryDto;
import com.github.b3kt.application.dto.pazaauto.SummaryDto.MekanikDailyDto;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.SummaryReportRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class SummaryService {

    @Inject
    SummaryReportRepository repo;

    public SummaryDto getSummary(String startDateStr, String endDateStr, String statusPembelianFilter) {
        Date startDate = Date.valueOf(LocalDate.parse(startDateStr));
        Date endDate = Date.valueOf(LocalDate.parse(endDateStr));
        List<String> statuses = parseStatusFilter(statusPembelianFilter);

        SummaryDto dto = new SummaryDto();

        dto.setTotalCustomers(repo.countDistinctSpk(startDate, endDate));
        dto.setTotalIncome(repo.sumPenjualanGrandTotal(startDate, endDate));
        dto.setTotalOutcome(repo.sumPembelianGrandTotal(startDate, endDate, statuses));
        dto.setNetProfit(dto.getTotalIncome().subtract(dto.getTotalOutcome()));
        dto.setTotalItemTerjual(repo.sumItemTerjual(startDate, endDate));

        dto.setDailyBreakdown(buildDailyBreakdown(startDate, endDate, statuses));
        dto.setSoldItemsBreakdown(mapSoldItems(repo.findSoldItemsPerDay(startDate, endDate)));
        dto.setJasaSummaryBreakdown(mapJasaSummary(repo.findJasaSummaryPerDay(startDate, endDate)));
        dto.setTopItems(mapTopItems(repo.findTopItems(startDate, endDate)));
        dto.setIncomeByMethod(mapIncomeByMethod(repo.findIncomeByMethod(startDate, endDate)));
        dto.setOutcomeByType(mapOutcomeByType(repo.findOutcomeByType(startDate, endDate, statuses)));
        dto.setMekanikSummary(mapMekanikSummary(repo.findMekanikSummary(startDate, endDate)));
        dto.setMekanikBreakdown(mapMekanikPerDay(repo.findMekanikPerDay(startDate, endDate)));

        return dto;
    }

    private List<DailyBreakdownDto> buildDailyBreakdown(Date startDate, Date endDate, List<String> statuses) {
        List<Object[]> incomeRows = repo.findDailyIncome(startDate, endDate);
        List<Object[]> outcomeRows = repo.findDailyOutcome(startDate, endDate, statuses);

        Map<String, BigDecimal> outcomeByDate = new LinkedHashMap<>();
        for (Object[] row : outcomeRows) {
            outcomeByDate.put(row[0].toString(), new BigDecimal(row[1].toString()));
        }

        List<DailyBreakdownDto> result = new ArrayList<>();
        for (Object[] row : incomeRows) {
            DailyBreakdownDto day = new DailyBreakdownDto();
            day.setDate(row[0].toString());
            day.setCustomers(((Number) row[1]).longValue());
            day.setIncome(new BigDecimal(row[2].toString()));
            day.setItemsTerjual(((Number) row[3]).longValue());
            BigDecimal outcome = outcomeByDate.getOrDefault(day.getDate(), BigDecimal.ZERO);
            day.setOutcome(outcome);
            day.setNet(day.getIncome().subtract(outcome));
            result.add(day);
        }
        return result;
    }

    private List<SoldItemDailyDto> mapSoldItems(List<Object[]> rows) {
        List<SoldItemDailyDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            SoldItemDailyDto item = new SoldItemDailyDto();
            item.setDate(row[0].toString());
            item.setSparepartId(row[1] != null ? ((Number) row[1]).longValue() : null);
            item.setNamaBarang(row[2] != null ? row[2].toString() : "");
            item.setTotalQty(((Number) row[3]).longValue());
            item.setTotalValue(new BigDecimal(row[4].toString()));
            item.setTotalNilaiAdjustment(new BigDecimal(row[5].toString()));
            item.setTotalModal(row[6] != null ? new BigDecimal(row[6].toString()) : BigDecimal.ZERO);
            result.add(item);
        }
        return result;
    }

    private List<JasaSummaryDailyDto> mapJasaSummary(List<Object[]> rows) {
        List<JasaSummaryDailyDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            JasaSummaryDailyDto item = new JasaSummaryDailyDto();
            item.setDate(row[0].toString());
            item.setJasaId(row[1] != null ? ((Number) row[1]).longValue() : null);
            item.setNamaJasa(row[2] != null ? row[2].toString() : "");
            item.setTotalQty(((Number) row[3]).longValue());
            item.setTotalNilai(new BigDecimal(row[4].toString()));
            item.setTotalNilaiAdjustment(new BigDecimal(row[5].toString()));
            item.setTotalModal(row[6] != null ? new BigDecimal(row[6].toString()) : BigDecimal.ZERO);
            result.add(item);
        }
        return result;
    }

    private List<TopItemDto> mapTopItems(List<Object[]> rows) {
        List<TopItemDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            TopItemDto item = new TopItemDto();
            item.setSparepartId(row[0] != null ? ((Number) row[0]).longValue() : null);
            item.setNamaBarang(row[1] != null ? row[1].toString() : "");
            item.setTotalQty(((Number) row[2]).longValue());
            item.setTotalValue(new BigDecimal(row[3].toString()));
            result.add(item);
        }
        return result;
    }

    private List<IncomeByMethodDto> mapIncomeByMethod(List<Object[]> rows) {
        List<IncomeByMethodDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            IncomeByMethodDto item = new IncomeByMethodDto();
            item.setLabel(row[0].toString());
            item.setAmount(new BigDecimal(row[1].toString()));
            result.add(item);
        }
        return result;
    }

    private List<OutcomeByTypeDto> mapOutcomeByType(List<Object[]> rows) {
        List<OutcomeByTypeDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            OutcomeByTypeDto item = new OutcomeByTypeDto();
            item.setLabel(row[0].toString());
            item.setAmount(new BigDecimal(row[1].toString()));
            result.add(item);
        }
        return result;
    }

    private List<MekanikSummaryDto> mapMekanikSummary(List<Object[]> rows) {
        List<MekanikSummaryDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            MekanikSummaryDto item = new MekanikSummaryDto();
            item.setMekanikId(((Number) row[0]).longValue());
            item.setNamaMekanik(row[1].toString());
            item.setTotalCustomers(((Number) row[2]).longValue());
            item.setTotalHari(((Number) row[3]).longValue());
            item.setRataPerHari(item.getTotalHari() > 0
                    ? BigDecimal.valueOf((double) item.getTotalCustomers() / item.getTotalHari())
                        .setScale(1, RoundingMode.HALF_UP).doubleValue()
                    : 0);
            result.add(item);
        }
        return result;
    }

    private List<MekanikDailyDto> mapMekanikPerDay(List<Object[]> rows) {
        List<MekanikDailyDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            MekanikDailyDto item = new MekanikDailyDto();
            item.setDate(row[0].toString());
            item.setMekanikId(((Number) row[1]).longValue());
            item.setNamaMekanik(row[2].toString());
            item.setTotalCustomers(((Number) row[3]).longValue());
            result.add(item);
        }
        return result;
    }

    private static List<String> parseStatusFilter(String filter) {
        if (filter == null || filter.isBlank()) return List.of();
        return Arrays.stream(filter.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
