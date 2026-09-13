package com.github.b3kt.infrastructure.persistence.repository.pazaauto;

import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPelangganKendaraanEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class TbPelangganKendaraanRepository implements PanacheRepositoryBase<TbPelangganKendaraanEntity, Long> {

    public Optional<TbPelangganKendaraanEntity> findCurrentByNopol(String nopol) {
        if (nopol == null || nopol.trim().isEmpty()) {
            return Optional.empty();
        }
        return find("nopol = ?1 and current = true", nopol).firstResultOptional();
    }

    public Optional<TbPelangganKendaraanEntity> findCurrentByNopolAndPelanggan(String nopol, Long pelangganId) {
        return find("nopol = ?1 and current = true and pelangganId = ?2", nopol, pelangganId)
                .firstResultOptional();
    }

    public Optional<TbPelangganKendaraanEntity> findExisting(String nopol, Long pelangganId, Long kendaraanId) {
        return find("nopol = ?1 and pelangganId = ?2 and kendaraanId = ?3", nopol, pelangganId, kendaraanId)
                .firstResultOptional();
    }

    public List<TbPelangganKendaraanEntity> findByNopolOrderByTanggalMulai(String nopol) {
        return find("nopol = ?1 order by tanggalMulai", nopol).list();
    }

    public List<TbPelangganKendaraanEntity> findByPelangganId(Long pelangganId) {
        return find("pelangganId = ?1 order by tanggalMulai", pelangganId).list();
    }

    public List<TbPelangganKendaraanEntity> findByKendaraanIdOrderByTanggalMulai(Long kendaraanId) {
        return find("kendaraanId = ?1 order by tanggalMulai", kendaraanId).list();
    }

    public List<TbPelangganKendaraanEntity> findByPelangganIdCurrent(Long pelangganId) {
        return find("pelangganId = ?1 and current = true order by tanggalMulai", pelangganId).list();
    }

    /**
     * Ownership row for a nopol that was active on the given date.
     */
    public Optional<TbPelangganKendaraanEntity> findByNopolActiveOn(String nopol, LocalDate date) {
        return find("nopol = ?1 and tanggalMulai <= ?2 and (tanggalAkhir is null or tanggalAkhir >= ?2) order by tanggalMulai",
                nopol, date, date).firstResultOptional();
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
    }

    @Transactional
    public TbPelangganKendaraanEntity openOwnership(Long pelangganId, Long kendaraanId, String nopol,
                                                     LocalDate tanggalMulai, String keterangan) {
        TbPelangganKendaraanEntity row = new TbPelangganKendaraanEntity();
        row.setPelangganId(pelangganId);
        row.setKendaraanId(kendaraanId);
        row.setNopol(nopol);
        row.setTanggalMulai(tanggalMulai);
        row.setTanggalAkhir(null);
        row.setCurrent(true);
        row.setKeterangan(keterangan);
        getEntityManager().persist(row);
        return row;
    }
}