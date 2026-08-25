package com.github.b3kt.presentation.rest.pazaauto;

import com.github.b3kt.application.dto.ApiResponse;
import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.application.dto.pazaauto.KaryawanDto;
import com.github.b3kt.application.mapper.pazaauto.KaryawanMapper;
import com.github.b3kt.application.service.pazaauto.TbKaryawanService;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbKaryawanEntity;
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

@RequestScoped
@Path("/api/pazaauto/karyawan")
public class TbKaryawanResource {

    @Inject
    TbKaryawanService service;

    @Inject
    KaryawanMapper karyawanMapper;

    @GET
    public Response findAll() {
        return Response.ok(ApiResponse.success(karyawanMapper.toDtoList(service.findAll()))).build();
    }

    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") String id) {
        TbKaryawanEntity entity = service.findById(Long.valueOf(id));
        if (entity == null) {
            return Response.ok(ApiResponse.error("Karyawan not found")).build();
        }
        return Response.ok(ApiResponse.success(karyawanMapper.toDto(entity))).build();
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

        PageResponse<TbKaryawanEntity> pageResponse = service.findPaginated(pageRequest);
        return Response.ok(ApiResponse.success(
                new PageResponse<>(
                        karyawanMapper.toDtoList(pageResponse.getRows()),
                        pageResponse.getCurrentPage(),
                        pageResponse.getRowsPerPage(),
                        pageResponse.getTotalRows()))).build();
    }

    @GET
    @Path("/unregistered")
    public Response listUnregistered() {
        return Response.ok(ApiResponse.success(karyawanMapper.toDtoList(service.findAllUnregistered()))).build();
    }

    @POST
    public Response create(KaryawanDto dto) {
        TbKaryawanEntity entity = karyawanMapper.toEntity(dto);
        TbKaryawanEntity created = service.create(entity);
        return Response.ok(ApiResponse.success("Karyawan created", karyawanMapper.toDto(created))).build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") String id, KaryawanDto dto) {
        TbKaryawanEntity entity = karyawanMapper.toEntity(dto);
        TbKaryawanEntity updated = service.update(Long.valueOf(id), entity);
        if (updated == null) {
            return Response.ok(ApiResponse.error("Karyawan not found")).build();
        }
        return Response.ok(ApiResponse.success("Karyawan updated", karyawanMapper.toDto(updated))).build();
    }

    @jakarta.ws.rs.DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") String id) {
        service.delete(Long.valueOf(id));
        return Response.ok(ApiResponse.success("Karyawan deleted")).build();
    }
}
