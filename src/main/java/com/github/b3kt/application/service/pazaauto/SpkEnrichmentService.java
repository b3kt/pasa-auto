package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPelangganEntity;
import com.github.b3kt.infrastructure.persistence.entity.subentity.SpkMekanik;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbKaryawanRepository;
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

    @Inject
    public SpkEnrichmentService(TbPelangganRepository pelangganRepository, TbKaryawanRepository karyawanRepository) {
        this.pelangganRepository = pelangganRepository;
        this.karyawanRepository = karyawanRepository;
    }

    public void enrich(SpkEnrichable target) {
        if (target.getPelangganId() != null) {
            TbPelangganEntity pelanggan = pelangganRepository.findById(target.getPelangganId());
            if (pelanggan != null) {
                target.setNamaPelanggan(pelanggan.getNamaPelanggan());
                target.setAlamatPelanggan(pelanggan.getAlamat());
                target.setMerkKendaraan(pelanggan.getMerk());
                target.setJenisKendaraan(pelanggan.getJenis());
            }
        } else if (target.getNopol() != null) {
            TbPelangganEntity pelanggan = pelangganRepository.find("nopol", target.getNopol()).firstResult();
            if (pelanggan != null) {
                target.setPelangganId(pelanggan.getId());
                target.setNamaPelanggan(pelanggan.getNamaPelanggan());
                target.setAlamatPelanggan(pelanggan.getAlamat());
                target.setMerkKendaraan(pelanggan.getMerk());
                target.setJenisKendaraan(pelanggan.getJenis());
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

    public void fillRequiredFields(List<? extends SpkEnrichable> entities) {
        entities.forEach(this::enrich);
    }
}