package com.github.b3kt.presentation.rest.pazaauto;

import com.github.b3kt.application.dto.ApiResponse;
import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.application.dto.pazaauto.SupplierDto;
import com.github.b3kt.application.mapper.pazaauto.SupplierMapper;
import com.github.b3kt.application.service.pazaauto.TbSupplierService;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSupplierEntity;
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
@Path("/api/pazaauto/supplier")
public class TbSupplierResource {

    @Inject
    TbSupplierService service;

    @Inject
    SupplierMapper supplierMapper;

    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") String id) {
        TbSupplierEntity entity = service.findById(Integer.valueOf(id));
        if (entity == null) {
            return Response.ok(ApiResponse.error("Supplier not found")).build();
        }
        return Response.ok(ApiResponse.success(supplierMapper.toDto(entity))).build();
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

        PageResponse<TbSupplierEntity> pageResponse = service.findPaginated(pageRequest);
        return Response.ok(ApiResponse.success(
                new PageResponse<>(
                        supplierMapper.toDtoList(pageResponse.getRows()),
                        pageResponse.getCurrentPage(),
                        pageResponse.getRowsPerPage(),
                        pageResponse.getTotalRows()))).build();
    }

    @GET
    @Path("/search")
    public Response search(@QueryParam("q") String query) {
        return Response.ok(ApiResponse.success(supplierMapper.toDtoList(service.search(query)))).build();
    }

    @POST
    public Response create(SupplierDto dto) {
        TbSupplierEntity entity = supplierMapper.toEntity(dto);
        TbSupplierEntity created = service.create(entity);
        return Response.ok(ApiResponse.success("Supplier created", supplierMapper.toDto(created))).build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") String id, SupplierDto dto) {
        TbSupplierEntity entity = supplierMapper.toEntity(dto);
        TbSupplierEntity updated = service.update(Integer.valueOf(id), entity);
        if (updated == null) {
            return Response.ok(ApiResponse.error("Supplier not found")).build();
        }
        return Response.ok(ApiResponse.success("Supplier updated", supplierMapper.toDto(updated))).build();
    }

    @jakarta.ws.rs.DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") String id) {
        service.delete(Integer.valueOf(id));
        return Response.ok(ApiResponse.success("Supplier deleted")).build();
    }
}
