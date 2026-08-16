package com.github.b3kt.presentation.rest.pazaauto;

import com.github.b3kt.application.dto.ApiResponse;
import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.application.dto.pazaauto.SparepartDto;
import com.github.b3kt.application.mapper.pazaauto.SparepartMapper;
import com.github.b3kt.application.service.pazaauto.TbSparepartService;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSparepartEntity;
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
@Path("/api/pazaauto/sparepart")
public class TbSparepartResource {

    @Inject
    TbSparepartService service;

    @Inject
    SparepartMapper sparepartMapper;

    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") String id) {
        TbSparepartEntity entity = service.findById(Long.valueOf(id));
        if (entity == null) {
            return Response.ok(ApiResponse.error("Sparepart not found")).build();
        }
        return Response.ok(ApiResponse.success(sparepartMapper.toDto(entity))).build();
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
            @QueryParam("supplierId") Long supplierId) {

        PageRequest pageRequest = new PageRequest(page, rowsPerPage);
        pageRequest.setSortBy(sortBy);
        pageRequest.setDescending(descending);
        pageRequest.setSearch(search);
        pageRequest.setStatusFilter(statusFilter);
        pageRequest.setFilterToday(filterToday);
        pageRequest.setSupplierId(supplierId);

        PageResponse<TbSparepartEntity> pageResponse = service.findPaginated(pageRequest);
        return Response.ok(ApiResponse.success(
                new PageResponse<>(
                        sparepartMapper.toDtoList(pageResponse.getRows()),
                        pageResponse.getCurrentPage(),
                        pageResponse.getRowsPerPage(),
                        pageResponse.getTotalRows()))).build();
    }

    @POST
    public Response create(SparepartDto dto) {
        TbSparepartEntity entity = sparepartMapper.toEntity(dto);
        TbSparepartEntity created = service.create(entity);
        return Response.ok(ApiResponse.success("Sparepart created", sparepartMapper.toDto(created))).build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") String id, SparepartDto dto) {
        TbSparepartEntity entity = sparepartMapper.toEntity(dto);
        TbSparepartEntity updated = service.update(Long.valueOf(id), entity);
        if (updated == null) {
            return Response.ok(ApiResponse.error("Sparepart not found")).build();
        }
        return Response.ok(ApiResponse.success("Sparepart updated", sparepartMapper.toDto(updated))).build();
    }

    @jakarta.ws.rs.DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") String id) {
        service.delete(Long.valueOf(id));
        return Response.ok(ApiResponse.success("Sparepart deleted")).build();
    }
}
