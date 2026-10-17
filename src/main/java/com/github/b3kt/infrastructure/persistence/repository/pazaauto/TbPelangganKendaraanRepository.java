package com.github.b3kt.infrastructure.persistence.repository.pazaauto;

import com.github.b3kt.application.helper.NopolNormalizer;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPelangganKendaraanEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class TbPelangganKendaraanRepository implements PanacheRepositoryBase<TbPelangganKendaraanEntity, Long> {

    @Inject
    EntityManager em;

    /**
     * Selectable vehicles: one row per nopol with its current owner and vehicle master.
     * Pelanggan rows that have no ownership row yet (legacy data) are appended so they
     * stay selectable. Columns: nopol, nama_pelanggan, alamat, merk, jenis, id_pelanggan, id_kendaraan.
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> findVehicleOptions() {
        String sql = """
                SELECT k.nopol,
                       p.nama_pelanggan,
                       p.alamat,
                       COALESCE(kd.merk, p.merk),
                       COALESCE(kd.jenis, p.jenis),
                       k.id_pelanggan,
                       k.id_kendaraan
                FROM tb_pelanggan_kendaraan k
                JOIN tb_pelanggan p ON p.id = k.id_pelanggan AND p.deleted_at IS NULL
                LEFT JOIN tb_kendaraan kd ON kd.id = k.id_kendaraan AND kd.deleted_at IS NULL
                WHERE k.is_current = true AND k.deleted_at IS NULL
                UNION ALL
                SELECT p.nopol,
                       p.nama_pelanggan,
                       p.alamat,
                       p.merk,
                       p.jenis,
                       p.id,
                       NULL::bigint
                FROM tb_pelanggan p
                WHERE p.deleted_at IS NULL
                  AND btrim(p.nopol) <> ''
                  AND NOT EXISTS (SELECT 1 FROM tb_pelanggan_kendaraan k
                                  WHERE regexp_replace(upper(k.nopol), '[[:space:]]', '', 'g')
                                        = regexp_replace(upper(p.nopol), '[[:space:]]', '', 'g')
                                    AND k.is_current = true AND k.deleted_at IS NULL)
                ORDER BY 1
                """;
        return em.createNativeQuery(sql).getResultList();
    }

    public Optional<TbPelangganKendaraanEntity> findCurrentByNopol(String nopol) {
        String norm = NopolNormalizer.normalize(nopol);
        if (norm.isEmpty()) {
            return Optional.empty();
        }
        return find("nopol = ?1 and current = true and deletedAt is null", norm).firstResultOptional();
    }

    public Optional<TbPelangganKendaraanEntity> findCurrentByNopolAndPelanggan(String nopol, Long pelangganId) {
        String norm = NopolNormalizer.normalize(nopol);
        if (norm.isEmpty()) {
            return Optional.empty();
        }
        return find("nopol = ?1 and current = true and pelangganId = ?2 and deletedAt is null", norm, pelangganId)
                .firstResultOptional();
    }

    public Optional<TbPelangganKendaraanEntity> findExisting(String nopol, Long pelangganId, Long kendaraanId) {
        return find("nopol = ?1 and pelangganId = ?2 and kendaraanId = ?3 and deletedAt is null",
                NopolNormalizer.normalize(nopol), pelangganId, kendaraanId)
                .firstResultOptional();
    }

    public List<TbPelangganKendaraanEntity> findByNopolOrderByTanggalMulai(String nopol) {
        return find("nopol = ?1 and deletedAt is null order by tanggalMulai", NopolNormalizer.normalize(nopol)).list();
    }

    public List<TbPelangganKendaraanEntity> findByPelangganId(Long pelangganId) {
        return find("pelangganId = ?1 and deletedAt is null order by tanggalMulai", pelangganId).list();
    }

    public List<TbPelangganKendaraanEntity> findByKendaraanIdOrderByTanggalMulai(Long kendaraanId) {
        return find("kendaraanId = ?1 and deletedAt is null order by tanggalMulai", kendaraanId).list();
    }

    public List<TbPelangganKendaraanEntity> findByPelangganIdCurrent(Long pelangganId) {
        return find("pelangganId = ?1 and current = true and deletedAt is null order by tanggalMulai", pelangganId).list();
    }

    /**
     * Ownership row for a nopol that was active on the given date.
     */
    public Optional<TbPelangganKendaraanEntity> findByNopolActiveOn(String nopol, LocalDate date) {
        return find("nopol = ?1 and tanggalMulai <= ?2 and (tanggalAkhir is null or tanggalAkhir >= ?2) and deletedAt is null order by tanggalMulai",
                NopolNormalizer.normalize(nopol), date, date).firstResultOptional();
    }

    @Transactional
    public void closeOwnership(Long id, LocalDate until, String keterangan) {
        findByIdOptional(id).ifPresent(row -> {
            row.setTanggalAkhir(until);
            row.setCurrent(false);
            if (keterangan != null && !keterangan.isBlank()) {
                row.setKeterangan(keterangan);
            }
            getEntityManager().merge(row);
        });
        // Flush now: within one flush Hibernate runs inserts before updates, so a following
        // openOwnership() insert would still see this row as current and break the partial
        // unique index idx_pk_current_nopol (nopol where is_current = true).
        getEntityManager().flush();
    }

    @Transactional
    public TbPelangganKendaraanEntity openOwnership(Long pelangganId, Long kendaraanId, String nopol,
                                                     LocalDate tanggalMulai, String keterangan) {
        TbPelangganKendaraanEntity row = new TbPelangganKendaraanEntity();
        row.setPelangganId(pelangganId);
        row.setKendaraanId(kendaraanId);
        row.setNopol(NopolNormalizer.normalize(nopol));
        row.setTanggalMulai(tanggalMulai);
        row.setTanggalAkhir(null);
        row.setCurrent(true);
        row.setKeterangan(keterangan);
        getEntityManager().persist(row);
        return row;
    }
}