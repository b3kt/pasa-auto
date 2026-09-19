package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.application.dto.pazaauto.KendaraanOwnershipDto;
import com.github.b3kt.application.dto.pazaauto.PelangganHistoryDto;
import com.github.b3kt.application.dto.pazaauto.VehicleHistoryDto;
import com.github.b3kt.application.dto.pazaauto.VehicleTransactionDto;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbKendaraanEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbMerkKendaraanEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPelangganEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPelangganKendaraanEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPenjualanEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSpkEntity;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbKendaraanRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbMerkKendaraanRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbPelangganKendaraanRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbPelangganRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbPenjualanRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbSpkRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@ApplicationScoped
public class KendaraanOwnershipService {

    @Inject
    TbPelangganKendaraanRepository ownershipRepository;

    @Inject
    TbKendaraanRepository kendaraanRepository;

    @Inject
    TbMerkKendaraanRepository merkRepository;

    @Inject
    TbPelangganRepository pelangganRepository;

    @Inject
    TbSpkRepository spkRepository;

    @Inject
    TbPenjualanRepository penjualanRepository;

    @Transactional
    public KendaraanOwnershipDto transfer(String nopol, Long idPelangganBaru,
                                          LocalDate tanggalAwal, String keterangan) {
        Optional<TbPelangganKendaraanEntity> currentOpt = ownershipRepository.findCurrentByNopol(nopol);
        LocalDate today = LocalDate.now();
        LocalDate mulai = tanggalAwal != null ? tanggalAwal : today;

        TbPelangganKendaraanEntity current = currentOpt.orElseThrow(
                () -> new IllegalArgumentException("No active ownership found for nopol: " + nopol));

        TbPelangganEntity newOwner = pelangganRepository.findByIdCached(idPelangganBaru)
                .orElseThrow(() -> new IllegalArgumentException("Pelanggan not found: " + idPelangganBaru));

        if (Objects.equals(current.getPelangganId(), idPelangganBaru)) {
            throw new IllegalArgumentException("Vehicle already owned by pelanggan " + idPelangganBaru);
        }

        if (mulai.isBefore(current.getTanggalMulai())) {
            throw new IllegalArgumentException("Transfer date cannot precede current ownership start");
        }

        ownershipRepository.closeOwnership(current.getId(), mulai.minusDays(1), keterangan);
        TbPelangganKendaraanEntity opened = ownershipRepository.openOwnership(
                idPelangganBaru, current.getKendaraanId(), nopol, mulai, keterangan);

        return toDto(opened);
    }

    public KendaraanOwnershipDto getVehicleByNopol(String nopol) {
        return ownershipRepository.findCurrentByNopol(nopol)
                .map(this::toDto)
                .orElse(null);
    }

    @Transactional
    public KendaraanOwnershipDto attach(Long pelangganId, String nopol, String merk, String jenis,
                                        LocalDate tanggalMulai, String keterangan) {
        if (nopol == null || nopol.isBlank()) {
            throw new IllegalArgumentException("nopol is required");
        }
        TbPelangganEntity owner = pelangganRepository.findByIdCached(pelangganId)
                .orElseThrow(() -> new IllegalArgumentException("Pelanggan not found: " + pelangganId));

        if (ownershipRepository.findCurrentByNopol(nopol).isPresent()) {
            throw new IllegalArgumentException("Vehicle " + nopol + " is already owned by another customer");
        }

        TbKendaraanEntity master = findOrCreateMaster(merk, jenis);
        TbPelangganKendaraanEntity opened = ownershipRepository.openOwnership(
                pelangganId, master.getId(), nopol,
                tanggalMulai != null ? tanggalMulai : LocalDate.now(), keterangan);
        return toDto(opened);
    }

    public VehicleHistoryDto getVehicleHistory(String nopol) {
        List<TbPelangganKendaraanEntity> rows = ownershipRepository.findByNopolOrderByTanggalMulai(nopol);
        if (rows.isEmpty()) {
            return null;
        }
        TbPelangganKendaraanEntity first = rows.get(0);
        TbKendaraanEntity master = kendaraanRepository.findByIdCached(first.getKendaraanId()).orElse(null);

        VehicleHistoryDto dto = new VehicleHistoryDto();
        dto.setNopol(nopol);
        if (master != null) {
            dto.setIdKendaraan(master.getId());
            dto.setMerk(master.getMerk());
            dto.setJenis(master.getJenis());
            dto.setModel(master.getModel());
        }
        dto.setOwners(rows.stream().map(this::toDto).toList());
        dto.setTransactions(findTransactionsByNopol(nopol));
        return dto;
    }

    public List<KendaraanOwnershipDto> listOwnerHistoryByMasterId(Long kendaraanId) {
        return ownershipRepository.findByKendaraanIdOrderByTanggalMulai(kendaraanId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public List<KendaraanOwnershipDto> listVehiclesByPelanggan(Long pelangganId) {
        return ownershipRepository.findByPelangganId(pelangganId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public List<KendaraanOwnershipDto> listCurrentVehiclesByPelanggan(Long pelangganId) {
        return ownershipRepository.findByPelangganIdCurrent(pelangganId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public PelangganHistoryDto getPelangganHistory(Long pelangganId) {
        TbPelangganEntity p = pelangganRepository.findByIdCached(pelangganId).orElse(null);
        if (p == null) {
            return null;
        }
        PelangganHistoryDto dto = new PelangganHistoryDto();
        dto.setIdPelanggan(p.getId());
        dto.setNamaPelanggan(p.getNamaPelanggan());
        dto.setAlamat(p.getAlamat());
        dto.setNoHp(p.getNoHp());
        dto.setVehicles(listVehiclesByPelanggan(pelangganId));
        dto.setTransactions(findTransactionsByPelanggan(pelangganId));
        return dto;
    }

    public List<VehicleTransactionDto> findTransactionsByNopol(String nopol) {
        List<VehicleTransactionDto> result = new ArrayList<>();
        List<TbSpkEntity> spks = spkRepository.find("nopol", nopol).list();
        if (spks != null) {
            for (TbSpkEntity spk : spks) {
                result.add(transactionsFor(spk));
            }
        }
        return result;
    }

    public List<VehicleTransactionDto> findTransactionsByPelanggan(Long pelangganId) {
        List<VehicleTransactionDto> result = new ArrayList<>();
        List<TbSpkEntity> spks = spkRepository.find("pelangganId", pelangganId).list();
        if (spks != null) {
            for (TbSpkEntity spk : spks) {
                result.add(transactionsFor(spk));
            }
        }
        List<TbPenjualanEntity> penjualans = penjualanRepository.find("pelangganId", pelangganId).list();
        if (penjualans != null) {
            for (TbPenjualanEntity p : penjualans) {
                VehicleTransactionDto dto = baseDto("PENJUALAN");
                dto.setNoPenjualan(p.getNoPenjualan());
                dto.setNoSpk(p.getNoSpk());
                dto.setStatus(p.getStatusPembayaran());
                dto.setGrandTotal(p.getGrandTotal());
                if (p.getTanggalJamPenjualan() != null) {
                    dto.setTanggal(p.getTanggalJamPenjualan().toInstant()
                            .atZone(ZoneId.systemDefault()).toLocalDateTime());
                    dto.setTanggalOnly(dto.getTanggal().toLocalDate());
                } else if (p.getCreatedAt() != null) {
                    dto.setTanggal(p.getCreatedAt());
                    dto.setTanggalOnly(p.getCreatedAt().toLocalDate());
                }
                Long ownerId = p.getPelangganId();
                if (ownerId != null) {
                    dto.setOwnerPelangganId(ownerId);
                    pelangganRepository.findByIdCached(ownerId)
                            .ifPresent(pel -> dto.setOwnerNamaPelanggan(pel.getNamaPelanggan()));
                } else if (p.getNoSpk() != null) {
                    resolveOwnerFromSpk(dto, p.getNoSpk());
                }
                result.add(dto);
            }
        }
        return result;
    }

    private void resolveOwnerFromSpk(VehicleTransactionDto dto, String noSpk) {
        TbSpkEntity spk = spkRepository.find("noSpk", noSpk).firstResult();
        if (spk == null) {
            return;
        }
        if (spk.getPelangganId() != null) {
            dto.setOwnerPelangganId(spk.getPelangganId());
            pelangganRepository.findByIdCached(spk.getPelangganId())
                    .ifPresent(p -> dto.setOwnerNamaPelanggan(p.getNamaPelanggan()));
            return;
        }
        if (spk.getNopol() != null && dto.getTanggalOnly() != null) {
            resolveOwner(dto, spk.getNopol(), dto.getTanggalOnly());
        }
        if (dto.getOwnerPelangganId() == null && spk.getNamaPelanggan() != null) {
            dto.setOwnerNamaPelanggan(spk.getNamaPelanggan());
        }
    }

    private VehicleTransactionDto transactionsFor(TbSpkEntity spk) {
        VehicleTransactionDto dto = baseDto("SPK");
        dto.setNoSpk(spk.getNoSpk());
        dto.setStatus(spk.getStatusSpk());
        LocalDateTime tanggal = parseTanggal(spk.getTanggalJamSpk() != null ? spk.getTanggalJamSpk() : null);
        if (tanggal == null && spk.getCreatedAt() != null) {
            tanggal = spk.getCreatedAt();
        }
        dto.setTanggal(tanggal);
        if (tanggal != null) {
            dto.setTanggalOnly(tanggal.toLocalDate());
        }

        if (spk.getPelangganId() != null) {
            dto.setOwnerPelangganId(spk.getPelangganId());
            pelangganRepository.findByIdCached(spk.getPelangganId())
                    .ifPresent(p -> dto.setOwnerNamaPelanggan(p.getNamaPelanggan()));
        } else if (spk.getNamaPelanggan() != null) {
            dto.setOwnerNamaPelanggan(spk.getNamaPelanggan());
        }

        // Link a matching penjualan if present so totals appear in history.
        TbPenjualanEntity penjualan = penjualanRepository.find("noSpk", spk.getNoSpk()).firstResult();
        if (penjualan != null) {
            dto.setNoPenjualan(penjualan.getNoPenjualan());
            dto.setGrandTotal(penjualan.getGrandTotal());
        }
        return dto;
    }

    private void resolveOwner(VehicleTransactionDto dto, String nopol, LocalDate tanggal) {
        if (nopol == null || tanggal == null) {
            return;
        }
        ownershipRepository.findByNopolActiveOn(nopol, tanggal)
                .ifPresent(owner -> {
                    dto.setOwnerPelangganId(owner.getPelangganId());
                    pelangganRepository.findByIdCached(owner.getPelangganId())
                            .ifPresent(p -> dto.setOwnerNamaPelanggan(p.getNamaPelanggan()));
                });
    }

    private VehicleTransactionDto baseDto(String jenis) {
        VehicleTransactionDto dto = new VehicleTransactionDto();
        dto.setJenis(jenis);
        dto.setGrandTotal(BigDecimal.ZERO);
        return dto;
    }

    private TbKendaraanEntity findOrCreateMaster(String merk, String jenis) {
        return kendaraanRepository.findByMerkAndJenis(merk, jenis).orElseGet(() -> {
            TbMerkKendaraanEntity merkMaster = merkRepository.findOrCreateByNama(merk);
            TbKendaraanEntity e = new TbKendaraanEntity();
            e.setMerkId(merkMaster.getId());
            e.setMerk(merkMaster.getNama());
            e.setJenis(jenis == null ? "" : jenis.trim());
            return kendaraanRepository.getEntityManager().merge(e);
        });
    }

    private LocalDateTime parseTanggal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String v = value.trim();
        for (java.time.format.DateTimeFormatter fmt : new java.time.format.DateTimeFormatter[]{
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"),
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")}) {
            try {
                if (v.length() <= 10) {
                    return java.time.LocalDate.parse(v, fmt).atStartOfDay();
                }
                return java.time.LocalDateTime.parse(v, fmt);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private KendaraanOwnershipDto toDto(TbPelangganKendaraanEntity row) {
        KendaraanOwnershipDto dto = new KendaraanOwnershipDto();
        dto.setId(row.getId());
        dto.setNopol(row.getNopol());
        dto.setTanggalMulai(row.getTanggalMulai());
        dto.setTanggalAkhir(row.getTanggalAkhir());
        dto.setCurrent(row.isCurrent());
        dto.setKeterangan(row.getKeterangan());
        dto.setIdKendaraan(row.getKendaraanId());
        dto.setIdPelanggan(row.getPelangganId());

        kendaraanRepository.findByIdCached(row.getKendaraanId()).ifPresent(k -> {
            dto.setMerk(k.getMerk());
            dto.setJenis(k.getJenis());
            dto.setModel(k.getModel());
        });
        pelangganRepository.findByIdCached(row.getPelangganId()).ifPresent(p -> {
            dto.setNamaPelanggan(p.getNamaPelanggan());
            dto.setAlamatPelanggan(p.getAlamat());
            dto.setNoHpPelanggan(p.getNoHp());
        });
        return dto;
    }
}