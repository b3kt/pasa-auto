package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.application.dto.pazaauto.PenjualanPrintDto;
import com.github.b3kt.application.helper.PageHelper;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.*;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.*;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class TbPenjualanService extends AbstractCrudService<TbPenjualanEntity, String> {

    @Inject
    TbPenjualanRepository repository;

    @Inject
    TbPenjualanDetailRepository penjualanDetailRepository;

    @Inject
    TbSpkRepository spkRepository;

    @Inject
    TbSpkService spkService;

    @Inject
    TbSpkDetailService spkDetailService;

    @Inject
    TbPelangganService pelangganService;

    @Inject
    TbKendaraanRepository kendaraanRepository;

    @Inject
    TbKaryawanRepository karyawanRepository;

    @Inject
    TbJasaRepository jasaRepository;

    @Inject
    TbSparepartRepository sparepartRepository;

    @Inject
    TbBarangRepository barangRepository;

    @Override
    protected PanacheRepositoryBase<TbPenjualanEntity, String> getRepository() {
        return repository;
    }

    @Override
    protected void setEntityId(TbPenjualanEntity entity, String id) {
        entity.setNoPenjualan(id);
    }

    public TbPenjualanEntity findByNoPenjualan(String noPenjualan) {
        TbPenjualanEntity entity = repository.findByNoPenjualan(noPenjualan);
        if (entity != null) {
            fillTransientFields(entity);
        }
        return entity;
    }

    public PenjualanPrintDto buildPrintDto(String noPenjualan) {
        TbPenjualanEntity penjualan = repository.find("noPenjualan", noPenjualan).firstResult();
        if (penjualan == null) return null;

        TbSpkEntity spk = penjualan.getNoSpk() != null
                ? spkRepository.find("noSpk", penjualan.getNoSpk()).firstResult()
                : null;

        PenjualanPrintDto dto = new PenjualanPrintDto();
        dto.setNoPenjualan(penjualan.getNoPenjualan());
        dto.setTanggal(new SimpleDateFormat("dd-MM-yyyy HH:mm").format(penjualan.getTanggalJamPenjualan()));
        dto.setNoSpk(penjualan.getNoSpk());
        dto.setStatusPembayaran(penjualan.getStatusPembayaran());
        dto.setMetodePembayaran(penjualan.getMetodePembayaran());
        dto.setDiskon(penjualan.getDiscount());
        dto.setGrandTotal(penjualan.getGrandTotal());
        dto.setUangDibayar(penjualan.getUangDibayar());
        dto.setKembalian(penjualan.getKembalian());

        // Customer Lookup
        Long pelangganId = penjualan.getPelangganId();
        if (pelangganId == null && spk != null) {
            pelangganId = spk.getPelangganId();
        }
        if (pelangganId != null) {
            TbPelangganEntity pelanggan = pelangganService.findById(pelangganId);
            if (pelanggan != null) {
                dto.setNamaPelanggan(pelanggan.getNamaPelanggan());
                dto.setAlamatPelanggan(pelanggan.getAlamat());
                dto.setNoHpPelanggan(pelanggan.getNoHp());
            }
        }

        if (spk != null) {
            dto.setKm(spk.getKmSaatIni());

            if (penjualan.getKendaraanId() != null) {
                TbKendaraanEntity kendaraan = kendaraanRepository.findById(penjualan.getKendaraanId());
                if (kendaraan != null) {
                    dto.setNopol(spk.getNopol());
                    dto.setMerk(kendaraan.getMerk());
                    dto.setModel(kendaraan.getModel());
                }
            } else if (spk.getNopol() != null) {
                dto.setNopol(spk.getNopol());
                TbPelangganEntity p = pelangganService.findByNopol(spk.getNopol());
                if (p != null) {
                    dto.setMerk(p.getMerk());
                    dto.setModel(p.getJenis());
                }
            }

            if (spk.getMekanikList() != null && !spk.getMekanikList().isEmpty()) {
                List<Long> ids = spk.getMekanikList().stream()
                        .map(com.github.b3kt.infrastructure.persistence.entity.subentity.SpkMekanik::getId)
                        .collect(Collectors.toList());
                String names = karyawanRepository.find("id in ?1", ids).stream()
                        .map(TbKaryawanEntity::getNamaKaryawan)
                        .collect(Collectors.joining(", "));
                dto.setNamaMekanik(names);
            }

            TbSpkEntity fullSpk = spkService.findById(spk.getId());
            List<PenjualanPrintDto.ItemDto> items = new ArrayList<>();
            BigDecimal subTotalCalc = BigDecimal.ZERO;

            if (fullSpk != null && fullSpk.getDetails() != null) {
                for (TbSpkDetailEntity detail : fullSpk.getDetails()) {
                    PenjualanPrintDto.ItemDto item = new PenjualanPrintDto.ItemDto();
                    item.setQty(detail.getJumlah());

                    BigDecimal price = BigDecimal.ZERO;
                    String type = "UNKNOWN";

                    if (detail.getJasaId() != null) {
                        TbJasaEntity jasa = jasaRepository.findById(detail.getJasaId());
                        if (jasa != null) {
                            price = BigDecimal.valueOf(jasa.getHargaJasa());
                            type = "JASA";
                        }
                    } else if (detail.getSparepartId() != null) {
                        TbSparepartEntity sparepart = sparepartRepository.findById(detail.getSparepartId());
                        if (sparepart != null) {
                            price = sparepart.getHargaJual();
                            type = "BARANG";
                        } else {
                            TbBarangEntity barang = barangRepository.findById(detail.getSparepartId());
                            if (barang != null) {
                                price = barang.getHargaJual();
                                type = "BARANG";
                            }
                        }
                    }

                    item.setHarga(price);
                    item.setType(type);
                    BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(detail.getJumlah()));
                    item.setSubTotal(lineTotal);
                    items.add(item);
                    subTotalCalc = subTotalCalc.add(lineTotal);
                }
            }
            dto.setItems(items);
            dto.setSubTotal(subTotalCalc);
        }

        return dto;
    }

    private void fillTransientFields(TbPenjualanEntity entity) {
        Long pelangganId = null;
        if (Objects.isNull(entity.getPelangganId())) {
            TbSpkEntity spkEntity = spkService.findByNoSpk(entity.getNoSpk());
            if (spkEntity != null) {
                pelangganId = spkEntity.getPelangganId();
            }

            List<TbSpkDetailEntity> details = spkDetailService.findByNoSpk(entity.getNoSpk());
            Optional.ofNullable(details).ifPresent(entity::setDetails);
        }

        if (Objects.nonNull(pelangganId)) {
            Long id = pelangganId;
            Optional.ofNullable(pelangganService.findById(id))
                    .ifPresent(pelanggan -> {
                        entity.setNamaPelanggan(pelanggan.getNamaPelanggan());
                        entity.setAlamatPelanggan(pelanggan.getAlamat());
                        entity.setMerkKendaraan(pelanggan.getMerk());
                        entity.setJenisKendaraan(pelanggan.getJenis());
                        entity.setNoPolisi(pelanggan.getNopol());
                    });
        }
    }

    @Transactional
    public TbPenjualanEntity createWithNoSpkValidation(TbPenjualanEntity entity) {
        TbPenjualanEntity existing = repository.find("noSpk", entity.getNoSpk()).firstResult();
        if (existing != null) {
            throw new IllegalArgumentException("No SPK " + entity.getNoSpk() + " already exists in penjualan records");
        }
        return create(entity);
    }

    @Transactional
    public TbPenjualanEntity updateWithNoSpkValidation(TbPenjualanEntity entity) {
        TbPenjualanEntity existing = repository.find("noSpk = ?1 AND noPenjualan <> ?2",
            entity.getNoSpk(), entity.getNoPenjualan()).firstResult();
        if (existing != null) {
            throw new IllegalArgumentException("No SPK " + entity.getNoSpk() + " already exists in other penjualan records");
        }
        return update(entity.getNoPenjualan(), entity);
    }

    @Override
    public com.github.b3kt.application.dto.PageResponse<TbPenjualanEntity> findPaginated(com.github.b3kt.application.dto.PageRequest pageRequest) {
        StringBuilder queryStr = new StringBuilder("1=1");
        io.quarkus.panache.common.Parameters params = new io.quarkus.panache.common.Parameters();

        if (pageRequest.getSearch() != null && !pageRequest.getSearch().isEmpty()) {
            queryStr.append(" and (lower(noPenjualan) like :search or lower(noSpk) like :search)");
            params.and("search", "%" + pageRequest.getSearch().toLowerCase() + "%");
        }

        if (pageRequest.getStatusFilter() != null && !pageRequest.getStatusFilter().isEmpty()) {
            String[] statuses = pageRequest.getStatusFilter().split(",");
            queryStr.append(" and statusPembayaran in (:statuses)");
            params.and("statuses", java.util.Arrays.asList(statuses));
        }

        if (pageRequest.getStartDate() != null && !pageRequest.getStartDate().isEmpty()) {
            try {
                java.time.LocalDate startDate = java.time.LocalDate.parse(pageRequest.getStartDate());
                java.util.Date startDateTime = java.util.Date.from(startDate.atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toInstant());
                queryStr.append(" and tanggalJamPenjualan >= :startDate");
                params.and("startDate", startDateTime);
            } catch (Exception e) {}
        }
        if (pageRequest.getEndDate() != null && !pageRequest.getEndDate().isEmpty()) {
            try {
                java.time.LocalDate endDate = java.time.LocalDate.parse(pageRequest.getEndDate());
                java.util.Date endDateTime = java.util.Date.from(endDate.atTime(java.time.LocalTime.MAX).atZone(java.time.ZoneId.systemDefault()).toInstant());
                queryStr.append(" and tanggalJamPenjualan <= :endDate");
                params.and("endDate", endDateTime);
            } catch (Exception e) {}
        }

        io.quarkus.hibernate.orm.panache.PanacheQuery<TbPenjualanEntity> query = repository.find(queryStr.toString(), params);
        return PageHelper.applyPagination(query, repository, pageRequest, queryStr.toString(), params);
    }

    @Transactional
    public void cancelPenjualanBySpk(String noSpk) {
        TbPenjualanEntity penjualan = repository.find("noSpk", noSpk).firstResult();
        if (penjualan == null) {
            throw new IllegalArgumentException("Penjualan not found for SPK: " + noSpk);
        }

        penjualanDetailRepository.delete("noPenjualan", penjualan.getNoPenjualan());
        repository.delete(penjualan);

        TbSpkEntity spk = spkRepository.find("noSpk", noSpk).firstResult();
        if (spk != null) {
            spk.setStatusSpk("OPEN");
            spk.setFinishedAt(null);
            spkRepository.getEntityManager().merge(spk);
        }
    }
}
