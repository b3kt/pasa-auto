package com.github.b3kt.infrastructure.persistence.repository.pazaauto;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.List;

@ApplicationScoped
public class SummaryReportRepository {

    @Inject
    EntityManager em;

    public long countDistinctSpk(Date startDate, Date endDate) {
        String sql = "SELECT COUNT(DISTINCT s.no_spk) " +
                "FROM tb_penjualan p " +
                "JOIN tb_spk s ON s.no_spk = p.no_spk " +
                "WHERE DATE(p.tgl_jam_penjualan) BETWEEN ?1 AND ?2";
        Number result = (Number) em.createNativeQuery(sql)
                .setParameter(1, startDate).setParameter(2, endDate)
                .getSingleResult();
        return result == null ? 0L : result.longValue();
    }

    public BigDecimal sumPenjualanGrandTotal(Date startDate, Date endDate) {
        String sql = "SELECT COALESCE(SUM(p.grand_total), 0) " +
                "FROM tb_penjualan p " +
                "JOIN tb_spk s ON s.no_spk = p.no_spk " +
                "WHERE DATE(p.tgl_jam_penjualan) BETWEEN ?1 AND ?2";
        Number result = (Number) em.createNativeQuery(sql)
                .setParameter(1, startDate).setParameter(2, endDate)
                .getSingleResult();
        return result == null ? BigDecimal.ZERO : new BigDecimal(result.toString());
    }

    public BigDecimal sumPembelianGrandTotal(Date startDate, Date endDate, List<String> statuses) {
        String sql = buildPembelianSumSql(statuses);
        Number result = (Number) em.createNativeQuery(sql)
                .setParameter(1, startDate).setParameter(2, endDate)
                .getSingleResult();
        return result == null ? BigDecimal.ZERO : new BigDecimal(result.toString());
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findDailyIncome(Date startDate, Date endDate) {
        String sql = "SELECT DATE(p.tgl_jam_penjualan) AS tanggal, " +
                "COUNT(DISTINCT s.no_spk) AS customers, " +
                "COALESCE(SUM(p.grand_total), 0) AS income, " +
                "COALESCE(SUM(d.jumlah), 0) AS items " +
                "FROM tb_penjualan p " +
                "JOIN tb_spk s ON s.no_spk = p.no_spk " +
                "LEFT JOIN tb_spk_detail d ON d.no_spk = s.no_spk AND d.id_sparepart IS NOT NULL " +
                "WHERE DATE(p.tgl_jam_penjualan) BETWEEN ?1 AND ?2 " +
                "GROUP BY DATE(p.tgl_jam_penjualan) " +
                "ORDER BY DATE(p.tgl_jam_penjualan) DESC";
        return em.createNativeQuery(sql)
                .setParameter(1, startDate).setParameter(2, endDate)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findDailyOutcome(Date startDate, Date endDate, List<String> statuses) {
        StringBuilder sql = new StringBuilder(
                "SELECT DATE(b.tgl_pembelian) AS tanggal, " +
                "COALESCE(SUM(b.grand_total), 0) AS outcome " +
                "FROM tb_pembelian b " +
                "WHERE DATE(b.tgl_pembelian) BETWEEN ?1 AND ?2");
        appendStatusFilter(sql, statuses);
        sql.append(" GROUP BY DATE(b.tgl_pembelian)");
        return em.createNativeQuery(sql.toString())
                .setParameter(1, startDate).setParameter(2, endDate)
                .getResultList();
    }

    public long sumItemTerjual(Date startDate, Date endDate) {
        String sql = "SELECT COALESCE(SUM(d.jumlah), 0) " +
                "FROM tb_spk_detail d " +
                "JOIN tb_spk s ON s.no_spk = d.no_spk " +
                "JOIN tb_penjualan p ON p.no_spk = s.no_spk " +
                "WHERE d.id_sparepart IS NOT NULL " +
                "AND DATE(p.tgl_jam_penjualan) BETWEEN ?1 AND ?2";
        Number result = (Number) em.createNativeQuery(sql)
                .setParameter(1, startDate).setParameter(2, endDate)
                .getSingleResult();
        return result == null ? 0L : result.longValue();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findSoldItemsPerDay(Date startDate, Date endDate) {
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
        return em.createNativeQuery(sql)
                .setParameter(1, startDate).setParameter(2, endDate)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findJasaSummaryPerDay(Date startDate, Date endDate) {
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
        return em.createNativeQuery(sql)
                .setParameter(1, startDate).setParameter(2, endDate)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findTopItems(Date startDate, Date endDate) {
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
        return em.createNativeQuery(sql)
                .setParameter(1, startDate).setParameter(2, endDate)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findIncomeByMethod(Date startDate, Date endDate) {
        String sql = "SELECT COALESCE(p.metode_pembayaran, 'LAINNYA') AS label, " +
                "COALESCE(SUM(p.grand_total), 0) AS amount " +
                "FROM tb_penjualan p " +
                "JOIN tb_spk s ON s.no_spk = p.no_spk " +
                "WHERE DATE(p.tgl_jam_penjualan) BETWEEN ?1 AND ?2 " +
                "GROUP BY COALESCE(p.metode_pembayaran, 'LAINNYA') " +
                "ORDER BY amount DESC";
        return em.createNativeQuery(sql)
                .setParameter(1, startDate).setParameter(2, endDate)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findOutcomeByType(Date startDate, Date endDate, List<String> statuses) {
        StringBuilder sql = new StringBuilder(
                "SELECT COALESCE(b.jenis_pembelian, 'LAINNYA') AS label, " +
                "COALESCE(SUM(b.grand_total), 0) AS amount " +
                "FROM tb_pembelian b " +
                "WHERE DATE(b.tgl_pembelian) BETWEEN ?1 AND ?2");
        appendStatusFilter(sql, statuses);
        sql.append(" GROUP BY COALESCE(b.jenis_pembelian, 'LAINNYA') ORDER BY amount DESC");
        return em.createNativeQuery(sql.toString())
                .setParameter(1, startDate).setParameter(2, endDate)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findMekanikSummary(Date startDate, Date endDate) {
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
        return em.createNativeQuery(sql)
                .setParameter(1, startDate).setParameter(2, endDate)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findMekanikPerDay(Date startDate, Date endDate) {
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
        return em.createNativeQuery(sql)
                .setParameter(1, startDate).setParameter(2, endDate)
                .getResultList();
    }

    private String buildPembelianSumSql(List<String> statuses) {
        StringBuilder sql = new StringBuilder(
                "SELECT COALESCE(SUM(b.grand_total), 0) " +
                "FROM tb_pembelian b " +
                "WHERE DATE(b.tgl_pembelian) BETWEEN ?1 AND ?2");
        appendStatusFilter(sql, statuses);
        return sql.toString();
    }

    private void appendStatusFilter(StringBuilder sql, List<String> statuses) {
        if (statuses != null && !statuses.isEmpty()) {
            sql.append(" AND b.status_pembayaran IN (");
            for (int i = 0; i < statuses.size(); i++) {
                sql.append("'").append(statuses.get(i).trim().replace("'", "''")).append("'");
                if (i < statuses.size() - 1) sql.append(",");
            }
            sql.append(")");
        }
    }
}
