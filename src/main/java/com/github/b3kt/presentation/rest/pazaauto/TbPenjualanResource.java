package com.github.b3kt.presentation.rest.pazaauto;

import com.github.b3kt.application.dto.ApiResponse;
import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.application.dto.pazaauto.PenjualanDto;
import com.github.b3kt.application.dto.pazaauto.PenjualanPrintDto;
import com.github.b3kt.application.mapper.pazaauto.PenjualanMapper;
import com.github.b3kt.application.service.pazaauto.TbPenjualanService;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPenjualanEntity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@RequestScoped
@Path("/api/pazaauto/penjualan")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TbPenjualanResource {

    @Inject
    TbPenjualanService service;

    @Inject
    PenjualanMapper penjualanMapper;

    @GET
    public Response findAll() {
        return Response.ok(ApiResponse.success(penjualanMapper.toDtoList(service.findAll()))).build();
    }

    @GET
    @Path("/{noPenjualan}/print")
    public Response print(@PathParam("noPenjualan") String noPenjualan) {
        PenjualanPrintDto dto = service.buildPrintDto(noPenjualan);
        if (dto == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(ApiResponse.success(dto)).build();
    }

    @GET
    @Path("/{noPenjualan}")
    public Response getById(@PathParam("noPenjualan") String noPenjualan) {
        TbPenjualanEntity entity = service.findByNoPenjualan(noPenjualan);
        if (entity == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ApiResponse.error("Penjualan not found"))
                    .build();
        }
        return Response.ok(ApiResponse.success(penjualanMapper.toDto(entity))).build();
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

        PageResponse<TbPenjualanEntity> pageResponse = service.findPaginated(pageRequest);
        return Response.ok(ApiResponse.success(
                new PageResponse<>(
                        penjualanMapper.toDtoList(pageResponse.getRows()),
                        pageResponse.getCurrentPage(),
                        pageResponse.getRowsPerPage(),
                        pageResponse.getTotalRows()))).build();
    }

    @POST
    public Response create(PenjualanDto dto) {
        TbPenjualanEntity entity = penjualanMapper.toEntity(dto);
        TbPenjualanEntity created = service.createWithNoSpkValidation(entity);
        return Response.ok(ApiResponse.success("Penjualan created", penjualanMapper.toDto(created))).build();
    }

    @PUT
    @Path("/{noPenjualan}")
    public Response update(@PathParam("noPenjualan") String noPenjualan, PenjualanDto dto) {
        TbPenjualanEntity entity = penjualanMapper.toEntity(dto);
        TbPenjualanEntity updated = service.updateWithNoSpkValidation(entity);
        return Response.ok(ApiResponse.success("Penjualan updated", penjualanMapper.toDto(updated))).build();
    }

    @DELETE
    @Path("/cancel-by-no-spk/{noSpk}")
    public Response cancelByNoSpk(@PathParam("noSpk") String noSpk) {
        service.cancelPenjualanBySpk(noSpk);
        return Response.ok(ApiResponse.success("Penjualan cancelled, SPK status reverted to OPEN")).build();
    }

    @DELETE
    @Path("/{noPenjualan}")
    public Response delete(@PathParam("noPenjualan") String noPenjualan) {
        service.delete(noPenjualan);
        return Response.ok(ApiResponse.success("Penjualan deleted")).build();
    }
}
