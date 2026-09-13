package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPelangganEntity;
import com.github.b3kt.infrastructure.persistence.entity.subentity.SpkMekanik;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbKaryawanRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbKendaraanRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbPelangganKendaraanRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbPelangganRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.stream.Collectors;

import io.quarkus.panache.common.Parameters;

@ApplicationScoped
public class SpkEnrichmentService {

    private final TbPelangganRepository pelangganRepository;
    private final TbKaryawanRepository karyawanRepository;
    private final TbKendaraanRepository kendaraanRepository;
    private final TbPelangganKendaraanRepository ownershipRepository;

    @Inject
    public SpkEnrichmentService(TbPelangganRepository pelangganRepository, TbKaryawanRepository karyawanRepository,
                                TbKendaraanRepository kendaraanRepository,
                                TbPelangganKendaraanRepository ownershipRepository) {
        this.pelangganRepository = pelangganRepository;
        this.karyawanRepository = karyawanRepository;
        this.kendaraanRepository = kendaraanRepository;
        this.ownershipRepository = ownershipRepository;
    }

    public void enrich(SpkEnrichable target) {
        if (target.getPelangganId() != null) {
            // Historical owner bound on the record - keep it as-is.
            TbPelangganEntity pelanggan = pelangganRepository.findById(target.getPelangganId());
            if (pelanggan != null) {
                target.setNamaPelanggan(pelanggan.getNamaPelanggan());
                target.setAlamatPelanggan(pelanggan.getAlamat());
                target.setMerkKendaraan(pelanggan.getMerk());
                target.setJenisKendaraan(pelanggan.getJenis());
            }
        } else if (target.getNopol() != null) {
            boolean resolved = resolveFromOwnership(target);
            if (!resolved) {
                TbPelangganEntity pelanggan = pelangganRepository.find("nopol", target.getNopol()).firstResult();
                if (pelanggan != null) {
                    target.setPelangganId(pelanggan.getId());
                    target.setNamaPelanggan(pelanggan.getNamaPelanggan());
                    target.setAlamatPelanggan(pelanggan.getAlamat());
                    target.setMerkKendaraan(pelanggan.getMerk());
                    target.setJenisKendaraan(pelanggan.getJenis());
                }
            }
        }

        if (target.getMekanikList() != null && !target.getMekanikList().isEmpty()) {
            List<Long> ids = target.getMekanikList().stream()
                    .map(SpkMekanik::getId).collect(Collectors.toList());
            List<String> names = karyawanRepository.find("id in :ids", Parameters.with("ids", ids))
                    .stream()
                    .map(obj -> obj.getNamaKaryawan())
                    .collect(Collectors.toList());
            target.setNamaKaryawan(String.join(", ", names));
        }

        if (target.getKm() != null) {
            target.setKmSaatIni(target.getKm());
        }
    }

    /**
     * Resolve vehicle/customer via the current ownership of the nopol.
     * Only used when the SPK has no bound pelangganId yet, so historical
     * transactions never get re-pointed at a later owner.
     */
    private boolean resolveFromOwnership(SpkEnrichable target) {
        String nopol = target.getNopol();
        if (nopol == null || nopol.isBlank()) {
            return false;
        }
        return ownershipRepository.findCurrentByNopol(nopol).map(row -> {
            target.setPelangganId(row.getPelangganId());
            pelangganRepository.findByIdCached(row.getPelangganId()).ifPresent(p -> {
                target.setNamaPelanggan(p.getNamaPelanggan());
                target.setAlamatPelanggan(p.getAlamat());
            });
            kendaraanRepository.findByIdCached(row.getKendaraanId()).ifPresent(k -> {
                target.setMerkKendaraan(k.getMerk());
                target.setJenisKendaraan(k.getJenis());
            });
            return true;
        }).orElse(false);
    }

    public void fillRequiredFields(List<? extends SpkEnrichable> entities) {
        entities.forEach(this::enrich);
    }
}