package com.github.b3kt.presentation.rest.pazaauto;

import com.github.b3kt.application.dto.ApiResponse;
import com.github.b3kt.application.dto.pazaauto.PelangganDto;
import com.github.b3kt.application.mapper.pazaauto.PelangganMapper;
import com.github.b3kt.application.service.pazaauto.TbPelangganService;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPelangganEntity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.core.Response;

@RequestScoped
@Path("/api/pazaauto/pelanggan")
public class TbPelangganResource {

    @Inject
    TbPelangganService service;

    @Inject
    PelangganMapper pelangganMapper;

    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") String id) {
        TbPelangganEntity entity = service.findById(Long.valueOf(id));
        if (entity == null) {
            return Response.ok(ApiResponse.error("Pelanggan not found")).build();
        }
        return Response.ok(ApiResponse.success(pelangganMapper.toDto(entity))).build();
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

        com.github.b3kt.application.dto.PageRequest pageRequest = new com.github.b3kt.application.dto.PageRequest(page, rowsPerPage);
        pageRequest.setSortBy(sortBy);
        pageRequest.setDescending(descending);
        pageRequest.setSearch(search);
        pageRequest.setStatusFilter(statusFilter);
        pageRequest.setFilterToday(filterToday);
        pageRequest.setStartDate(startDate);
        pageRequest.setEndDate(endDate);

        com.github.b3kt.application.dto.PageResponse<TbPelangganEntity> pageResponse = service.findPaginated(pageRequest);
        return Response.ok(ApiResponse.success(
                new com.github.b3kt.application.dto.PageResponse<>(
                        pelangganMapper.toDtoList(pageResponse.getRows()),
                        pageResponse.getCurrentPage(),
                        pageResponse.getRowsPerPage(),
                        pageResponse.getTotalRows()))).build();
    }

    @GET
    @Path("/by-nopol/{nopol}")
    public Response findByNopol(@PathParam("nopol") String nopol) {
        TbPelangganEntity pelanggan = service.findByNopol(nopol);
        if (pelanggan == null) {
            return Response.ok(ApiResponse.error("Pelanggan not found for nopol: " + nopol)).build();
        }
        return Response.ok(ApiResponse.success(pelangganMapper.toDto(pelanggan))).build();
    }

    @PUT
    @Path("/by-nopol/{nopol}")
    public Response updateByNopol(@PathParam("nopol") String nopol, PelangganDto pelangganData) {
        TbPelangganEntity entity = pelangganMapper.toEntity(pelangganData);
        TbPelangganEntity updated = service.patchByNopol(nopol, entity);
        if (updated == null) {
            return Response.ok(ApiResponse.error("Pelanggan not found for nopol: " + nopol)).build();
        }
        return Response.ok(ApiResponse.success("Pelanggan updated", pelangganMapper.toDto(updated))).build();
    }

    @POST
    public Response create(PelangganDto dto) {
        TbPelangganEntity entity = pelangganMapper.toEntity(dto);
        TbPelangganEntity created = service.create(entity);
        return Response.ok(ApiResponse.success("Pelanggan created", pelangganMapper.toDto(created))).build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") String id, PelangganDto dto) {
        TbPelangganEntity entity = pelangganMapper.toEntity(dto);
        TbPelangganEntity updated = service.update(Long.valueOf(id), entity);
        if (updated == null) {
            return Response.ok(ApiResponse.error("Pelanggan not found")).build();
        }
        return Response.ok(ApiResponse.success("Pelanggan updated", pelangganMapper.toDto(updated))).build();
    }

    @jakarta.ws.rs.DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") String id) {
        service.delete(Long.valueOf(id));
        return Response.ok(ApiResponse.success("Pelanggan deleted")).build();
    }
}
