package com.github.b3kt.presentation.rest.pazaauto;

import com.github.b3kt.application.dto.ApiResponse;
import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.application.dto.pazaauto.RekapPenjualanDto;
import com.github.b3kt.application.service.pazaauto.AbstractCrudService;
import com.github.b3kt.application.service.pazaauto.TbKaryawanService;
import com.github.b3kt.application.service.pazaauto.TbPelangganService;
import com.github.b3kt.application.service.pazaauto.TbSpkService;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbKaryawanEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPelangganEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSpkEntity;
import com.github.b3kt.infrastructure.persistence.entity.subentity.SpkMekanik;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@RequestScoped
@Path("/api/pazaauto/rekap-penjualan")
@RequiredArgsConstructor
public class TbRekapPenjualanResource extends AbstractCrudResource<TbSpkEntity, Long> {

    final TbSpkService service;
    final TbPelangganService pelangganService;
    final TbKaryawanService karyawanService;

    @Override
    protected AbstractCrudService<TbSpkEntity, Long> getService() {
        return service;
    }

    @Override
    protected Long parseId(String id) {
        return Long.valueOf(id);
    }

    @Override
    protected String getEntityName() {
        return "SPK";
    }

    @GET
    @Path("/by-no-spk/{noSpk}")
    public Response findByNoSpk(@PathParam("noSpk") String noSpk) {
        return Response.ok(ApiResponse.success(service.findByNoSpk(noSpk))).build();
    }

    @GET
    @Path("/unprocessed")
    public Response getUnprocessedSpk() {
        return Response.ok(ApiResponse.success(service.findUnprocessedSpk())).build();
    }

    @Override
    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") String id) {
        RekapPenjualanDto entity = service.findByIdWithPenjualan(parseId(id));

        fillKaryawanDetail(entity);
        fillPelangganDetail(entity);
        fillKendaraanDetail(entity);

        return Response.ok(ApiResponse.success(entity)).build();
    }

    @Override
    @GET
    @Path("/paginated")
    public Response listPaginated(
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("rowsPerPage") @DefaultValue("10") int rowsPerPage,
            @QueryParam("sortBy") String sortBy,
            @QueryParam("descending") @DefaultValue("false") boolean descending,
            @QueryParam("search") String search,
            @QueryParam("statusFilter") String statusFilter,
            @QueryParam("filterToday") @DefaultValue("false") boolean filterToday,
            @QueryParam("startDate") String startDate,
            @QueryParam("endDate") String endDate) {

        PageRequest pageRequest = new PageRequest(page, rowsPerPage);
        pageRequest.setSortBy(sortBy);
        pageRequest.setDescending(descending);
        pageRequest.setSearch(search);
        pageRequest.setStatusFilter(statusFilter);
        pageRequest.setFilterToday(filterToday);
        pageRequest.setStartDate(startDate);
        pageRequest.setEndDate(endDate);

        PageResponse<RekapPenjualanDto> pageResponse = service.findPaginatedWithPenjualan(pageRequest);
        return Response.ok(ApiResponse.success(pageResponse)).build();
    }

    @GET
    @Path("/get-next-spk-number")
    public Response getNextSpk() {
        String lastSpkNumber = service.getNextSpkNumber(spkNoformatter.format(LocalDateTime.now()));
        String lastQueueNumber = lastSpkNumber.substring(lastSpkNumber.length() - 2);
        int nextQueueNumber = Integer.parseInt(lastQueueNumber) + 1;
        String nextSpkNumber = lastSpkNumber.substring(0, lastSpkNumber.length() - 2)
                + String.format("%02d", nextQueueNumber);
        return Response.ok(ApiResponse.success(nextSpkNumber)).build();
    }

    @POST
    @Override
    public Response create(TbSpkEntity entity) {
        return Response.notModified().build();
    }

    @Override
    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") String id, TbSpkEntity entity) {
        return Response.notModified().build();
    }

    @Override
    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") String id) {
        TbSpkEntity entity = getService().findById(parseId(id));
        entity.setKeterangan("SPK Dibatalkan. lastStatus: " + entity.getStatusSpk());
        entity.setStatusSpk("BATAL");
        TbSpkEntity updated = getService().update(parseId(id), entity);

        return Response.ok(ApiResponse.success(getEntityName() + " cancelled", updated)).build();
    }

    private void fillPelangganDetail(RekapPenjualanDto entity) {

        TbPelangganEntity pelanggan;
        if (Objects.isNull(entity.getPelangganId())) {
            pelanggan = pelangganService.findByNopol(entity.getNopol());
            if (Objects.nonNull(pelanggan)) {
                entity.setPelangganId(pelanggan.getId());
                entity.setNamaPelanggan(pelanggan.getNamaPelanggan());
            }
        }else{
            pelanggan = pelangganService.findById(entity.getPelangganId());
        }

        if(Objects.nonNull(pelanggan)){
            entity.setAlamatPelanggan(pelanggan.getAlamat());
            entity.setMerkKendaraan(pelanggan.getMerk());
            entity.setJenisKendaraan(pelanggan.getJenis());
        }
    }

    private void fillKaryawanDetail(RekapPenjualanDto entity) {
        if (Objects.isNull(entity.getMekanikId())) {
            if (entity.getMekanikList() == null || entity.getMekanikList().isEmpty()) {
                return;
            }
            Long mekanikId = entity.getMekanikList().stream()
                    .findFirst()
                    .map(SpkMekanik::getId)
                    .orElse(null);
            if (mekanikId == null) {
                return;
            }
            TbKaryawanEntity karyawan = karyawanService.findById(mekanikId);
            if (Objects.nonNull(karyawan)) {
                entity.setNamaKaryawan(karyawan.getNamaKaryawan());
                entity.setMekanikId(karyawan.getId());
            }
        }
    }

    private void fillKendaraanDetail(RekapPenjualanDto entity) {
        if (Objects.nonNull(entity.getKm())) {
            entity.setKmSaatIni(entity.getKm());
        }
    }
}
