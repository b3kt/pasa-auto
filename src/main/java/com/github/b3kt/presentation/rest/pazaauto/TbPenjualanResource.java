package com.github.b3kt.presentation.rest.pazaauto;

import com.github.b3kt.application.service.pazaauto.AbstractCrudService;
import com.github.b3kt.application.service.pazaauto.TbPenjualanService;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPenjualanEntity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@RequestScoped
@Path("/api/pazaauto/penjualan")
public class TbPenjualanResource extends AbstractCrudResource<TbPenjualanEntity, String> {

    @Inject
    TbPenjualanService service;

    @Inject
    com.github.b3kt.application.service.pazaauto.TbSpkService spkService;

    @Inject
    com.github.b3kt.application.service.pazaauto.TbPelangganService pelangganService;

    @Inject
    com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbKendaraanRepository kendaraanRepository;

    @Override
    protected AbstractCrudService<TbPenjualanEntity, String> getService() {
        return service;
    }

    @Override
    protected String parseId(String id) {
        return id;
    }

    @Override
    protected String getEntityName() {
        return "Penjualan";
    }

    @Inject
    com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbSpkRepository spkRepository;

    @Inject
    com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbJasaRepository jasaRepository;

    @Inject
    com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbSparepartRepository sparepartRepository;

    @Inject
    com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbBarangRepository barangRepository;

    @Inject
    com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbPenjualanRepository penjualanRepository;

    @Inject
    com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbKaryawanRepository karyawanRepository;

    @GET
    @Path("/{noPenjualan}/print")
    public jakarta.ws.rs.core.Response print(@jakarta.ws.rs.PathParam("noPenjualan") String noPenjualan) {
        TbPenjualanEntity penjualan = penjualanRepository.find("noPenjualan", noPenjualan).firstResult();
        if (penjualan == null) {
            return jakarta.ws.rs.core.Response.status(jakarta.ws.rs.core.Response.Status.NOT_FOUND).build();
        }

        com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSpkEntity spk = null;
        if (penjualan.getNoSpk() != null) {
            spk = spkRepository.find("noSpk", penjualan.getNoSpk()).firstResult();
        }

        com.github.b3kt.application.dto.pazaauto.PenjualanPrintDto dto = new com.github.b3kt.application.dto.pazaauto.PenjualanPrintDto();
        dto.setNoPenjualan(penjualan.getNoPenjualan());
        dto.setTanggal(new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm").format(penjualan.getTanggalJamPenjualan()));
        dto.setNoSpk(penjualan.getNoSpk());
        dto.setStatusPembayaran(penjualan.getStatusPembayaran());
        dto.setMetodePembayaran(penjualan.getMetodePembayaran());
        dto.setGrandTotal(penjualan.getGrandTotal());
        dto.setUangDibayar(penjualan.getUangDibayar());
        dto.setKembalian(penjualan.getKembalian());
        dto.setDiskon(penjualan.getDiskon());
        dto.setPpn(penjualan.getPpn());

        // Customer Lookup
        Long pelangganId = penjualan.getPelangganId();
        if (pelangganId == null && spk != null) {
            pelangganId = spk.getPelangganId();
        }

        if (pelangganId != null) {
            com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPelangganEntity pelanggan = pelangganService
                    .findById(pelangganId);
            if (pelanggan != null) {
                dto.setNamaPelanggan(pelanggan.getNamaPelanggan());
                dto.setAlamatPelanggan(pelanggan.getAlamat());
                dto.setNoHpPelanggan(pelanggan.getNoHp());
            }
        }

        if (spk != null) {
            dto.setKm(spk.getKmSaatIni());

            // Vehicle Lookup
            if (penjualan.getKendaraanId() != null) {
                com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbKendaraanEntity kendaraan = kendaraanRepository
                        .findById(penjualan.getKendaraanId());
                if (kendaraan != null) {
                    dto.setNopol(spk.getNopol());
                    dto.setMerk(kendaraan.getMerk());
                    dto.setModel(kendaraan.getModel());
                }
            } else if (spk.getNopol() != null) {
                dto.setNopol(spk.getNopol());
                com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPelangganEntity p = pelangganService
                        .findByNopol(spk.getNopol());
                if (p != null) {
                    dto.setMerk(p.getMerk());
                    dto.setModel(p.getJenis());
                }
            }

            // Mechanic Lookup
            if (spk.getMekanikList() != null && !spk.getMekanikList().isEmpty()) {
                java.util.List<Long> ids = spk.getMekanikList().stream()
                        .map(com.github.b3kt.infrastructure.persistence.entity.subentity.SpkMekanik::getId)
                        .collect(java.util.stream.Collectors.toList());

                java.util.List<String> names = karyawanRepository.find("id in ?1", ids).stream()
                        .map(k -> k.getNamaKaryawan())
                        .collect(java.util.stream.Collectors.toList());

                dto.setNamaMekanik(String.join(", ", names));
            }

            // Items
            com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSpkEntity fullSpk = spkService
                    .findById(spk.getId());

            java.util.List<com.github.b3kt.application.dto.pazaauto.PenjualanPrintDto.ItemDto> items = new java.util.ArrayList<>();
            java.math.BigDecimal subTotalCalc = java.math.BigDecimal.ZERO;

            if (fullSpk != null && fullSpk.getDetails() != null) {
                for (com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSpkDetailEntity detail : fullSpk
                        .getDetails()) {
                    com.github.b3kt.application.dto.pazaauto.PenjualanPrintDto.ItemDto item = new com.github.b3kt.application.dto.pazaauto.PenjualanPrintDto.ItemDto();
                    item.setNama(detail.getId().getNamaJasa());
                    item.setQty(detail.getJumlah());

                    java.math.BigDecimal price = java.math.BigDecimal.ZERO;
                    String type = "UNKNOWN";

                    if (detail.getJasaId() != null) {
                        com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbJasaEntity jasa = jasaRepository
                                .findById(detail.getJasaId());
                        if (jasa != null) {
                            price = java.math.BigDecimal.valueOf(jasa.getHargaJasa());
                            type = "JASA";
                        }
                    } else if (detail.getSparepartId() != null) {
                        com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSparepartEntity sparepart = sparepartRepository
                                .findById(detail.getSparepartId());
                        if (sparepart != null) {
                            price = sparepart.getHargaJual();
                            type = "BARANG";
                        } else {
                            com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbBarangEntity barang = barangRepository
                                    .findById(detail.getSparepartId());
                            if (barang != null) {
                                price = barang.getHargaJual();
                                type = "BARANG";
                            }
                        }

                    }

                    item.setHarga(price);
                    item.setType(type);

                    java.math.BigDecimal lineTotal = price.multiply(java.math.BigDecimal.valueOf(detail.getJumlah()));
                    item.setSubTotal(lineTotal);

                    items.add(item);

                    subTotalCalc = subTotalCalc.add(lineTotal);
                }
            }
            dto.setItems(items);
            dto.setSubTotal(subTotalCalc);
        }

        return jakarta.ws.rs.core.Response.ok(com.github.b3kt.application.dto.ApiResponse.success(dto)).build();
    }
}
