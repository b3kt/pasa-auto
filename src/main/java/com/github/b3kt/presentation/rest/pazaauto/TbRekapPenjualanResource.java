package com.github.b3kt.presentation.rest.pazaauto;

import com.github.b3kt.application.dto.ApiResponse;
import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.application.dto.pazaauto.RekapPenjualanDto;
import com.github.b3kt.application.dto.pazaauto.SpkDto;
import com.github.b3kt.application.mapper.pazaauto.SpkMapper;
import com.github.b3kt.application.service.pazaauto.TbSpkService;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSpkEntity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

import java.time.LocalDateTime;

@RequestScoped
@Path("/api/pazaauto/rekap-penjualan")
public class TbRekapPenjualanResource {

    @Inject
    TbSpkService service;

    @Inject
    SpkMapper spkMapper;

    @GET
    @Path("/by-no-spk/{noSpk}")
    public Response findByNoSpk(@PathParam("noSpk") String noSpk) {
        TbSpkEntity entity = service.findByNoSpk(noSpk);
        if (entity == null) {
            return Response.ok(ApiResponse.error("SPK not found")).build();
        }
        return Response.ok(ApiResponse.success(spkMapper.toDto(entity))).build();
    }

    @GET
    @Path("/unprocessed")
    public Response getUnprocessedSpk() {
        return Response.ok(ApiResponse.success(spkMapper.toDtoList(service.findUnprocessedSpk()))).build();
    }

    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") String id) {
        RekapPenjualanDto entity = service.findByIdWithPenjualan(Long.valueOf(id));
        return Response.ok(ApiResponse.success(entity)).build();
    }

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
        String nextSpkNumber = service.generateNextSpkNumber(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDateTime.now()));
        return Response.ok(ApiResponse.success(nextSpkNumber)).build();
    }

    @jakarta.ws.rs.DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") String id) {
        TbSpkEntity cancelled = service.cancelSpk(Long.valueOf(id));
        if (cancelled == null) {
            return Response.ok(ApiResponse.error("SPK not found")).build();
        }
        return Response.ok(ApiResponse.success("SPK cancelled", spkMapper.toDto(cancelled))).build();
    }
}
