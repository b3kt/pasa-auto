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
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class SummaryService {

    @Inject
    EntityManager em;

    public SummaryDto getSummary(String startDateStr, String endDateStr, String statusPembelianFilter) {
        Date startDate = Date.valueOf(LocalDate.parse(startDateStr));
        Date endDate = Date.valueOf(LocalDate.parse(endDateStr));

        SummaryDto dto = new SummaryDto();

        dto.setTotalCustomers(queryTotalCustomers(startDate, endDate));
        dto.setTotalIncome(queryTotalIncome(startDate, endDate));
        dto.setTotalOutcome(queryTotalOutcome(startDate, endDate, statusPembelianFilter));
        dto.setNetProfit(dto.getTotalIncome().subtract(dto.getTotalOutcome()));
        dto.setTotalItemTerjual(queryTotalItemTerjual(startDate, endDate));

        dto.setDailyBreakdown(buildDailyBreakdown(startDate, endDate, statusPembelianFilter));
        dto.setSoldItemsBreakdown(querySoldItemsPerDay(startDate, endDate));
        dto.setJasaSummaryBreakdown(queryJasaSummaryPerDay(startDate, endDate));
        dto.setTopItems(queryTopItems(startDate, endDate));
        dto.setIncomeByMethod(queryIncomeByMethod(startDate, endDate));
        dto.setOutcomeByType(queryOutcomeByType(startDate, endDate, statusPembelianFilter));
        dto.setMekanikSummary(queryMekanikSummary(startDate, endDate));
        dto.setMekanikBreakdown(queryMekanikPerDay(startDate, endDate));

        return dto;
    }

    // ── Headline metrics ──────────────────────────────────────────────────

    private long queryTotalCustomers(Date startDate, Date endDate) {
        String sql = "SELECT COUNT(DISTINCT s.no_spk) " +
                "FROM tb_penjualan p " +
                "JOIN tb_spk s ON s.no_spk = p.no_spk " +
                "WHERE DATE(p.tgl_jam_penjualan) BETWEEN ?1 AND ?2";
        Number result = (Number) em.createNativeQuery(sql)
                .setParameter(1, startDate)
                .setParameter(2, endDate)
                .getSingleResult();
        return result == null ? 0L : result.longValue();
    }

    private BigDecimal queryTotalIncome(Date startDate, Date endDate) {
        String sql = "SELECT COALESCE(SUM(p.grand_total), 0) " +
                "FROM tb_penjualan p " +
                "JOIN tb_spk s ON s.no_spk = p.no_spk " +
                "WHERE DATE(p.tgl_jam_penjualan) BETWEEN ?1 AND ?2";
        Number result = (Number) em.createNativeQuery(sql)
                .setParameter(1, startDate)
                .setParameter(2, endDate)
                .getSingleResult();
        return result == null ? BigDecimal.ZERO : new BigDecimal(result.toString());
    }

    private BigDecimal queryTotalOutcome(Date startDate, Date endDate, String statusFilter) {
        StringBuilder sql = new StringBuilder(
                "SELECT COALESCE(SUM(b.grand_total), 0) " +
                "FROM tb_pembelian b " +
                "WHERE DATE(b.tgl_pembelian) BETWEEN ?1 AND ?2");
        appendStatusFilter(sql, statusFilter);
        Number result = (Number) em.createNativeQuery(sql.toString())
                .setParameter(1, startDate)
                .setParameter(2, endDate)
                .getSingleResult();
        return result == null ? BigDecimal.ZERO : new BigDecimal(result.toString());
    }

    private long queryTotalItemTerjual(Date startDate, Date endDate) {
        String sql = "SELECT COALESCE(SUM(d.jumlah), 0) " +
                "FROM tb_spk_detail d " +
                "JOIN tb_spk s ON s.no_spk = d.no_spk " +
                "JOIN tb_penjualan p ON p.no_spk = s.no_spk " +
                "WHERE d.id_sparepart IS NOT NULL " +
                "AND DATE(p.tgl_jam_penjualan) BETWEEN ?1 AND ?2";
        Number result = (Number) em.createNativeQuery(sql)
                .setParameter(1, startDate)
                .setParameter(2, endDate)
                .getSingleResult();
        return result == null ? 0L : result.longValue();
    }

    // ── Daily breakdown (income + outcome + customers + items merged by date) ──

    @SuppressWarnings("unchecked")
    private List<DailyBreakdownDto> buildDailyBreakdown(Date startDate, Date endDate, String statusFilter) {
        // income + customers per day
        String incomeSql = "SELECT DATE(p.tgl_jam_penjualan) AS tanggal, " +
                "COUNT(DISTINCT s.no_spk) AS customers, " +
                "COALESCE(SUM(p.grand_total), 0) AS income, " +
                "COALESCE(SUM(d.jumlah), 0) AS items " +
                "FROM tb_penjualan p " +
                "JOIN tb_spk s ON s.no_spk = p.no_spk " +
                "LEFT JOIN tb_spk_detail d ON d.no_spk = s.no_spk AND d.id_sparepart IS NOT NULL " +
                "WHERE DATE(p.tgl_jam_penjualan) BETWEEN ?1 AND ?2 " +
                "GROUP BY DATE(p.tgl_jam_penjualan) " +
                "ORDER BY DATE(p.tgl_jam_penjualan) DESC";

        List<Object[]> incomeRows = em.createNativeQuery(incomeSql)
                .setParameter(1, startDate)
                .setParameter(2, endDate)
                .getResultList();

        // outcome per day
        StringBuilder outcomeSql = new StringBuilder(
                "SELECT DATE(b.tgl_pembelian) AS tanggal, " +
                "COALESCE(SUM(b.grand_total), 0) AS outcome " +
                "FROM tb_pembelian b " +
                "WHERE DATE(b.tgl_pembelian) BETWEEN ?1 AND ?2");
        appendStatusFilter(outcomeSql, statusFilter);
        outcomeSql.append(" GROUP BY DATE(b.tgl_pembelian)");

        List<Object[]> outcomeRows = em.createNativeQuery(outcomeSql.toString())
                .setParameter(1, startDate)
                .setParameter(2, endDate)
                .getResultList();

        // index outcome by date
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

    // ── Per barang per hari ───────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<SoldItemDailyDto> querySoldItemsPerDay(Date startDate, Date endDate) {
        String sql = "SELECT DATE(p.tgl_jam_penjualan) AS tanggal, " +
                "d.id_sparepart, d.nama_jasa AS namaBarang, " +
                "SUM(d.jumlah) AS totalQty, " +
                "COALESCE(SUM(d.harga_master * d.jumlah), 0) AS totalValue, " +
                "COALESCE(SUM(d.harga * d.jumlah), 0) AS totalNilaiAdjustment, " +
                "COALESCE(SUM(b.harga_beli * d.jumlah), 0) AS totalModal " +
                "FROM tb_spk_detail d " +
                "JOIN tb_spk s ON s.no_spk = d.no_spk " +
                "JOIN tb_penjualan p ON p.no_spk = s.no_spk " +
                "LEFT JOIN tb_barang b ON b.id = d.id_sparepart " +
                "WHERE d.id_sparepart IS NOT NULL " +
                "AND DATE(p.tgl_jam_penjualan) BETWEEN ?1 AND ?2 " +
                "GROUP BY DATE(p.tgl_jam_penjualan), d.id_sparepart, d.nama_jasa " +
                "ORDER BY DATE(p.tgl_jam_penjualan) DESC, totalQty DESC";

        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter(1, startDate)
                .setParameter(2, endDate)
                .getResultList();

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

    @SuppressWarnings("unchecked")
    private List<JasaSummaryDailyDto> queryJasaSummaryPerDay(Date startDate, Date endDate) {
        String sql = "SELECT DATE(p.tgl_jam_penjualan) AS tanggal, " +
                "d.id_jasa, d.nama_jasa AS namaJasa, " +
                "SUM(d.jumlah) AS totalQty, " +
                "COALESCE(SUM(d.harga_master * d.jumlah), 0) AS totalNilai, " +
                "COALESCE(SUM(d.harga * d.jumlah), 0) AS totalNilaiAdjustment, " +
                "COALESCE(SUM(j.harga_jasa * d.jumlah), 0) AS totalModal " +
                "FROM tb_spk_detail d " +
                "JOIN tb_spk s ON s.no_spk = d.no_spk " +
                "JOIN tb_penjualan p ON p.no_spk = s.no_spk " +
                "LEFT JOIN tb_jasa j ON j.id = d.id_jasa " +
                "WHERE d.id_jasa IS NOT NULL " +
                "AND DATE(p.tgl_jam_penjualan) BETWEEN ?1 AND ?2 " +
                "GROUP BY DATE(p.tgl_jam_penjualan), d.id_jasa, d.nama_jasa " +
                "ORDER BY DATE(p.tgl_jam_penjualan) DESC, totalQty DESC";

        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter(1, startDate)
                .setParameter(2, endDate)
                .getResultList();

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

    // ── Top 10 items ─────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<TopItemDto> queryTopItems(Date startDate, Date endDate) {
        String sql = "SELECT d.id_sparepart, d.nama_jasa AS namaBarang, " +
                "SUM(d.jumlah) AS totalQty, " +
                "COALESCE(SUM(d.harga * d.jumlah), 0) AS totalValue " +
                "FROM tb_spk_detail d " +
                "JOIN tb_spk s ON s.no_spk = d.no_spk " +
                "JOIN tb_penjualan p ON p.no_spk = s.no_spk " +
                "WHERE d.id_sparepart IS NOT NULL " +
                "AND DATE(p.tgl_jam_penjualan) BETWEEN ?1 AND ?2 " +
                "GROUP BY d.id_sparepart, d.nama_jasa " +
                "ORDER BY totalQty DESC " +
                "LIMIT 10";

        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter(1, startDate)
                .setParameter(2, endDate)
                .getResultList();

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

    // ── Income by payment method ──────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<IncomeByMethodDto> queryIncomeByMethod(Date startDate, Date endDate) {
        String sql = "SELECT COALESCE(p.metode_pembayaran, 'LAINNYA') AS label, " +
                "COALESCE(SUM(p.grand_total), 0) AS amount " +
                "FROM tb_penjualan p " +
                "JOIN tb_spk s ON s.no_spk = p.no_spk " +
                "WHERE DATE(p.tgl_jam_penjualan) BETWEEN ?1 AND ?2 " +
                "GROUP BY COALESCE(p.metode_pembayaran, 'LAINNYA') " +
                "ORDER BY amount DESC";

        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter(1, startDate)
                .setParameter(2, endDate)
                .getResultList();

        List<IncomeByMethodDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            IncomeByMethodDto item = new IncomeByMethodDto();
            item.setLabel(row[0].toString());
            item.setAmount(new BigDecimal(row[1].toString()));
            result.add(item);
        }
        return result;
    }

    // ── Outcome by jenis pembelian ────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<OutcomeByTypeDto> queryOutcomeByType(Date startDate, Date endDate, String statusFilter) {
        StringBuilder sql = new StringBuilder(
                "SELECT COALESCE(b.jenis_pembelian, 'LAINNYA') AS label, " +
                "COALESCE(SUM(b.grand_total), 0) AS amount " +
                "FROM tb_pembelian b " +
                "WHERE DATE(b.tgl_pembelian) BETWEEN ?1 AND ?2");
        appendStatusFilter(sql, statusFilter);
        sql.append(" GROUP BY COALESCE(b.jenis_pembelian, 'LAINNYA') ORDER BY amount DESC");

        List<Object[]> rows = em.createNativeQuery(sql.toString())
                .setParameter(1, startDate)
                .setParameter(2, endDate)
                .getResultList();

        List<OutcomeByTypeDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            OutcomeByTypeDto item = new OutcomeByTypeDto();
            item.setLabel(row[0].toString());
            item.setAmount(new BigDecimal(row[1].toString()));
            result.add(item);
        }
        return result;
    }

    // ── Mechanic summary ─────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<MekanikSummaryDto> queryMekanikSummary(Date startDate, Date endDate) {
        String sql = "SELECT (m.value->>'id')::bigint AS mekanikId, " +
                "k.nama_karyawan AS namaMekanik, " +
                "COUNT(DISTINCT s.no_spk) AS totalCustomers, " +
                "COUNT(DISTINCT DATE(p.tgl_jam_penjualan)) AS totalHari " +
                "FROM tb_spk s " +
                "JOIN tb_penjualan p ON p.no_spk = s.no_spk " +
                "CROSS JOIN LATERAL jsonb_array_elements(s.mekanik_list) AS m(value) " +
                "JOIN tb_karyawan k ON k.id = (m.value->>'id')::bigint " +
                "WHERE DATE(p.tgl_jam_penjualan) BETWEEN ?1 AND ?2 " +
                "GROUP BY (m.value->>'id')::bigint, k.nama_karyawan " +
                "ORDER BY totalCustomers DESC";

        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter(1, startDate)
                .setParameter(2, endDate)
                .getResultList();

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

    // ── Mechanic per day ─────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<MekanikDailyDto> queryMekanikPerDay(Date startDate, Date endDate) {
        String sql = "SELECT DATE(p.tgl_jam_penjualan) AS tanggal, " +
                "(m.value->>'id')::bigint AS mekanikId, " +
                "k.nama_karyawan AS namaMekanik, " +
                "COUNT(DISTINCT s.no_spk) AS totalCustomers " +
                "FROM tb_spk s " +
                "JOIN tb_penjualan p ON p.no_spk = s.no_spk " +
                "CROSS JOIN LATERAL jsonb_array_elements(s.mekanik_list) AS m(value) " +
                "JOIN tb_karyawan k ON k.id = (m.value->>'id')::bigint " +
                "WHERE DATE(p.tgl_jam_penjualan) BETWEEN ?1 AND ?2 " +
                "GROUP BY DATE(p.tgl_jam_penjualan), (m.value->>'id')::bigint, k.nama_karyawan " +
                "ORDER BY DATE(p.tgl_jam_penjualan) DESC, totalCustomers DESC";

        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter(1, startDate)
                .setParameter(2, endDate)
                .getResultList();

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

    // ── Helper ────────────────────────────────────────────────────────────

    private void appendStatusFilter(StringBuilder sql, String statusFilter) {
        if (statusFilter != null && !statusFilter.isBlank()) {
            String[] statuses = statusFilter.split(",");
            sql.append(" AND b.status_pembayaran IN (");
            for (int i = 0; i < statuses.length; i++) {
                sql.append("'").append(statuses[i].trim().replace("'", "''")).append("'");
                if (i < statuses.length - 1) sql.append(",");
            }
            sql.append(")");
        }
    }
}
