package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbKendaraanEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPelangganKendaraanEntity;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbPelangganKendaraanRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * Keeps the ownership junction table in sync with reads/writes to the legacy
 * tb_pelanggan table (which still carries nopol/merk/jenis).
 */
@ApplicationScoped
public class PelangganOwnershipSyncService {

    @Inject
    TbKendaraanService kendaraanService;

    @Inject
    TbPelangganKendaraanRepository ownershipRepository;

    @Transactional
    public void syncOnCreate(Long pelangganId, String nopol, String merk, String jenis, LocalDate tanggalMulai) {
        if (nopol == null || nopol.isBlank() || pelangganId == null) {
            return;
        }
        // An existing active ownership for the same nopol wins (legacy data).
        if (ownershipRepository.findCurrentByNopol(nopol).isPresent()) {
            return;
        }
        TbKendaraanEntity master = kendaraanService.findOrCreateByMerkJenis(merk, jenis);
        ownershipRepository.openOwnership(pelangganId, master.getId(), nopol,
                tanggalMulai != null ? tanggalMulai : LocalDate.now(), null);
    }

    @Transactional
    public void syncOnUpdate(Long pelangganId, String oldNopol, String oldMerk, String oldJenis,
                             String newNopol, String newMerk, String newJenis, LocalDate tanggalMulai) {
        if (pelangganId == null) {
            return;
        }
        boolean nopolChanged = !Objects.equals(oldNopol, newNopol);
        boolean masterChanged = !Objects.equals(normalize(oldMerk), normalize(newMerk))
                || !Objects.equals(normalize(oldJenis), normalize(newJenis));

        if (nopolChanged) {
            if (oldNopol != null && !oldNopol.isBlank()) {
                ownershipRepository.findCurrentByNopolAndPelanggan(oldNopol, pelangganId)
                        .ifPresent(r -> ownershipRepository.closeOwnership(r.getId(),
                                LocalDate.now().minusDays(1), "nopol berubah"));
            }
            if (newNopol != null && !newNopol.isBlank()
                    && ownershipRepository.findCurrentByNopol(newNopol).isEmpty()) {
                TbKendaraanEntity master = kendaraanService.findOrCreateByMerkJenis(newMerk, newJenis);
                ownershipRepository.openOwnership(pelangganId, master.getId(), newNopol,
                        tanggalMulai != null ? tanggalMulai : LocalDate.now(), null);
            }
        } else if (masterChanged && newNopol != null && !newNopol.isBlank()) {
            TbKendaraanEntity master = kendaraanService.findOrCreateByMerkJenis(newMerk, newJenis);
            Optional<TbPelangganKendaraanEntity> current =
                    ownershipRepository.findCurrentByNopolAndPelanggan(newNopol, pelangganId);
            if (current.isPresent()) {
                TbPelangganKendaraanEntity r = current.get();
                r.setKendaraanId(master.getId());
                ownershipRepository.getEntityManager().merge(r);
            } else if (ownershipRepository.findCurrentByNopol(newNopol).isEmpty()) {
                // Legacy pelanggan without an ownership row yet: link it to the (new) master now.
                ownershipRepository.openOwnership(pelangganId, master.getId(), newNopol,
                        tanggalMulai != null ? tanggalMulai : LocalDate.now(), null);
            }
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}