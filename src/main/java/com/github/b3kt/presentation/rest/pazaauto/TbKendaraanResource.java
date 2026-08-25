package com.github.b3kt.presentation.rest.pazaauto;

import com.github.b3kt.application.dto.ApiResponse;
import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.application.dto.pazaauto.KendaraanDto;
import com.github.b3kt.application.mapper.pazaauto.KendaraanMapper;
import com.github.b3kt.application.service.pazaauto.TbKendaraanService;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbKendaraanEntity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.core.Response;

import java.util.List;

@RequestScoped
@Path("/api/pazaauto/kendaraan")
public class TbKendaraanResource {

    @Inject
    TbKendaraanService service;

    @Inject
    KendaraanMapper kendaraanMapper;

    @GET
    public Response findAll() {
        return Response.ok(ApiResponse.success(kendaraanMapper.toDtoList(service.findAll()))).build();
    }

    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") String id) {
        TbKendaraanEntity entity = service.findById(Long.valueOf(id));
        if (entity == null) {
            return Response.ok(ApiResponse.error("Kendaraan not found")).build();
        }
        return Response.ok(ApiResponse.success(kendaraanMapper.toDto(entity))).build();
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

        PageResponse<TbKendaraanEntity> pageResponse = service.findPaginated(pageRequest);
        return Response.ok(ApiResponse.success(
                new PageResponse<>(
                        kendaraanMapper.toDtoList(pageResponse.getRows()),
                        pageResponse.getCurrentPage(),
                        pageResponse.getRowsPerPage(),
                        pageResponse.getTotalRows()))).build();
    }

    @GET
    @Path("/merk/distinct")
    public Response getDistinctMerks() {
        List<String> merks = service.findDistinctMerks();
        return Response.ok(ApiResponse.success(merks)).build();
    }

    @GET
    @Path("/jenis/distinct")
    public Response getDistinctJenis() {
        List<String> jenis = service.findDistinctJenis();
        return Response.ok(ApiResponse.success(jenis)).build();
    }

    @GET
    @Path("/jenis/by-merk")
    public Response getDistinctJenisByMerk(@QueryParam("merk") String merk) {
        List<String> jenis;
        if (merk == null || merk.trim().isEmpty()) {
             jenis = service.findDistinctJenis();
        } else {
             jenis = service.findDistinctJenisByMerk(merk);
        }
        return Response.ok(ApiResponse.success(jenis)).build();
    }

    @POST
    public Response create(KendaraanDto dto) {
        TbKendaraanEntity entity = kendaraanMapper.toEntity(dto);
        TbKendaraanEntity created = service.create(entity);
        return Response.ok(ApiResponse.success("Kendaraan created", kendaraanMapper.toDto(created))).build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") String id, KendaraanDto dto) {
        TbKendaraanEntity entity = kendaraanMapper.toEntity(dto);
        TbKendaraanEntity updated = service.update(Long.valueOf(id), entity);
        if (updated == null) {
            return Response.ok(ApiResponse.error("Kendaraan not found")).build();
        }
        return Response.ok(ApiResponse.success("Kendaraan updated", kendaraanMapper.toDto(updated))).build();
    }

    @jakarta.ws.rs.DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") String id) {
        service.delete(Long.valueOf(id));
        return Response.ok(ApiResponse.success("Kendaraan deleted")).build();
    }
}
