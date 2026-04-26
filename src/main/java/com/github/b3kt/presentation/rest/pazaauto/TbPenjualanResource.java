package com.github.b3kt.presentation.rest.pazaauto;

import com.github.b3kt.application.dto.ApiResponse;
import com.github.b3kt.application.service.pazaauto.*;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPenjualanEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSpkDetailEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSpkEntity;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.*;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RequestScoped
@Path("/api/pazaauto/penjualan")
@RequiredArgsConstructor
public class TbPenjualanResource extends AbstractCrudResource<TbPenjualanEntity, String> {

    private final TbPenjualanService service;
    private final TbSpkService spkService;
    private final TbPelangganService pelangganService;
    private final TbKendaraanRepository kendaraanRepository;
    private final TbSpkRepository spkRepository;
    private final TbSpkDetailService spkDetailService;
    private final TbJasaRepository jasaRepository;
    private final TbSparepartRepository sparepartRepository;
    private final TbBarangRepository barangRepository;
    private final TbPenjualanRepository penjualanRepository;
    private final TbKaryawanRepository karyawanRepository;

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

    @Override
    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") String noPenjualan) {
        TbPenjualanEntity entity = service.findByNoPenjualan(noPenjualan);

        fillTransientFields(entity);
        return Response.ok(ApiResponse.success(entity)).build();
    }

    private void fillTransientFields(TbPenjualanEntity entity) {

        Long pelangganId = null;
        if(Objects.isNull(entity.getPelangganId())){
            TbSpkEntity spkEntity = spkService.findByNoSpk(entity.getNoSpk());
            pelangganId = spkEntity.getPelangganId();


            List<TbSpkDetailEntity> details = spkDetailService.findByNoSpk(entity.getNoSpk());
            Optional.ofNullable(details)
                    .ifPresent(list -> entity.setDetails(list));
        }

        if(Objects.nonNull(pelangganId)){
            Optional.ofNullable(pelangganService.findById(pelangganId))
                    .ifPresent(pelanggan ->  {
                        entity.setNamaPelanggan(pelanggan.getNamaPelanggan());
                        entity.setAlamatPelanggan(pelanggan.getAlamat());
                        entity.setMerkKendaraan(pelanggan.getMerk());
                        entity.setJenisKendaraan(pelanggan.getJenis());
                        entity.setNoPolisi(pelanggan.getNopol());
                    });
        }
    }

    @Override
    @POST
    public Response create(TbPenjualanEntity entity) {
        TbPenjualanEntity created = service.createWithNoSpkValidation(entity);
        return Response.ok(ApiResponse.success(getEntityName() + " created", created)).build();
    }

    @Override
    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") String id, TbPenjualanEntity entity) {
        TbPenjualanEntity updated = service.updateWithNoSpkValidation(entity);
        return Response.ok(ApiResponse.success(getEntityName() + " updated", updated)).build();
    }

    @DELETE
    @Path("/cancel-by-no-spk/{noSpk}")
    public Response cancelByNoSpk(@PathParam("noSpk") String noSpk) {
        service.cancelPenjualanBySpk(noSpk);
        return Response.ok(ApiResponse.success("Penjualan cancelled, SPK status reverted to OPEN")).build();
    }
}
