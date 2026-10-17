package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.application.helper.NopolNormalizer;
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
 * <p>When a customer owns several vehicles, the "primary" vehicle is the one whose plate matches
 * the legacy {@code tb_pelanggan.nopol} value; otherwise the most recently acquired current
 * vehicle wins. This keeps the single-vehicle API surface behaving as it did before.
 */
@ApplicationScoped
public class PelangganVehicleResolver {

    @Inject
    TbPelangganKendaraanRepository ownershipRepository;

    @Inject
    TbKendaraanRepository kendaraanRepository;

    private static final Comparator<TbPelangganKendaraanEntity> PRIMARY_ORDER = Comparator
            .comparingInt((TbPelangganKendaraanEntity row) -> row.getTanggalMulai() == null ? 1 : 0)
            .thenComparing(TbPelangganKendaraanEntity::getTanggalMulai,
                    Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(TbPelangganKendaraanEntity::getId,
                    Comparator.nullsLast(Comparator.naturalOrder()));

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
     * from its current ownership rows. No-op when the customer owns no vehicle, so placeholder
     * customers keep their existing values.
     */
    public void enrichVehicleInfo(TbPelangganEntity pelanggan) {
        if (pelanggan == null || pelanggan.getId() == null) {
            return;
        }
        List<TbPelangganKendaraanEntity> current = ownershipRepository.findByPelangganIdCurrent(pelanggan.getId());
        if (current.isEmpty()) {
            return;
        }
        String legacy = NopolNormalizer.normalize(pelanggan.getNopol());
        current.stream()
                .sorted(primaryComparator(legacy))
                .findFirst()
                .ifPresent(row -> applyVehicle(pelanggan, row));
    }

    private Comparator<TbPelangganKendaraanEntity> primaryComparator(String legacyNopol) {
        return Comparator
                .comparingInt((TbPelangganKendaraanEntity row) ->
                        NopolNormalizer.normalize(row.getNopol()).equals(legacyNopol) ? 0 : 1)
                .thenComparing(PRIMARY_ORDER);
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
