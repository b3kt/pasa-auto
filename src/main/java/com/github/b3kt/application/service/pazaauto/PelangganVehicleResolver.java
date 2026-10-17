package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbKendaraanEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPelangganEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPelangganKendaraanEntity;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbKendaraanRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbPelangganKendaraanRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Compatibility read layer (Phase 1) that fills the deprecated
 * {@code tb_pelanggan.nopol}/{@code merk}/{@code jenis} view fields from the authoritative
 * vehicle ownership junction ({@code tb_pelanggan_kendaraan}) while those columns are being
 * phased out.
 *
 * <p>When a customer owns several vehicles, the "primary" vehicle is the current one acquired most
 * recently (latest {@code tanggal_mulai}, ties to the highest id). This is the single rule shared
 * with the {@code trg_pk_sync_legacy_vehicle} trigger and the {@code v_pelanggan_legacy} view
 * (ADR-0011), so every reader of the single-vehicle view agrees on the same plate.
 */
@ApplicationScoped
public class PelangganVehicleResolver {

    @Inject
    TbPelangganKendaraanRepository ownershipRepository;

    @Inject
    TbKendaraanRepository kendaraanRepository;

    static final Comparator<TbPelangganKendaraanEntity> PRIMARY_ORDER = Comparator
            .comparing(TbPelangganKendaraanEntity::getTanggalMulai,
                    Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(TbPelangganKendaraanEntity::getId,
                    Comparator.nullsLast(Comparator.reverseOrder()));

    /**
     * Convenience lookup used by enrichment paths: resolve the current owner id of a plate
     * through the ownership junction (normalized), falling back to the caller when absent.
     */
    public Optional<Long> resolveOwnerPelangganId(String nopol) {
        return ownershipRepository.findCurrentByNopol(nopol)
                .map(TbPelangganKendaraanEntity::getPelangganId);
    }

    /**
     * Overwrite the deprecated vehicle view fields of {@code pelanggan} with the values derived
     * from its current ownership rows. No-op when the customer owns no vehicle (the derived columns
     * are already NULL for such a customer).
     */
    public void enrichVehicleInfo(TbPelangganEntity pelanggan) {
        if (pelanggan == null || pelanggan.getId() == null) {
            return;
        }
        List<TbPelangganKendaraanEntity> current = ownershipRepository.findByPelangganIdCurrent(pelanggan.getId());
        if (current.isEmpty()) {
            return;
        }
        current.stream()
                .min(PRIMARY_ORDER)
                .ifPresent(row -> applyVehicle(pelanggan, row));
    }

    private void applyVehicle(TbPelangganEntity pelanggan, TbPelangganKendaraanEntity row) {
        if (row.getNopol() != null && !row.getNopol().isBlank()) {
            pelanggan.setNopol(row.getNopol());
        }
        if (row.getKendaraanId() == null) {
            return;
        }
        kendaraanRepository.findByIdCached(row.getKendaraanId()).ifPresent(master -> {
            if (master.getMerk() != null) {
                pelanggan.setMerk(master.getMerk());
            }
            pelanggan.setJenis(master.getJenis());
        });
    }

    /**
     * Resolve the current vehicle master (merk/jenis/model) for a plate, for enrichment callers
     * that only have a nopol.
     */
    public Optional<TbKendaraanEntity> resolveVehicleMasterByNopol(String nopol) {
        return ownershipRepository.findCurrentByNopol(nopol)
                .map(TbPelangganKendaraanEntity::getKendaraanId)
                .flatMap(kendaraanRepository::findByIdCached);
    }
}
