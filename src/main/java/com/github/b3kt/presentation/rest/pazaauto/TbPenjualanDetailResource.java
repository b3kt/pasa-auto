package com.github.b3kt.presentation.rest.pazaauto;

import com.github.b3kt.application.dto.ApiResponse;
import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.application.dto.pazaauto.PenjualanDetailDto;
import com.github.b3kt.application.mapper.pazaauto.PenjualanDetailMapper;
import com.github.b3kt.application.service.pazaauto.TbPenjualanDetailService;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPenjualanDetailEntity;
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
@Path("/api/pazaauto/penjualan-detail")
public class TbPenjualanDetailResource {

    @Inject
    TbPenjualanDetailService service;

    @Inject
    PenjualanDetailMapper penjualanDetailMapper;

    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") String id) {
        TbPenjualanDetailEntity entity = service.findById(Long.valueOf(id));
        if (entity == null) {
            return Response.ok(ApiResponse.error("Penjualan Detail not found")).build();
        }
        return Response.ok(ApiResponse.success(penjualanDetailMapper.toDto(entity))).build();
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

        PageResponse<TbPenjualanDetailEntity> pageResponse = service.findPaginated(pageRequest);
        return Response.ok(ApiResponse.success(
                new PageResponse<>(
                        penjualanDetailMapper.toDtoList(pageResponse.getRows()),
                        pageResponse.getCurrentPage(),
                        pageResponse.getRowsPerPage(),
                        pageResponse.getTotalRows()))).build();
    }

    @POST
    public Response create(PenjualanDetailDto dto) {
        TbPenjualanDetailEntity entity = penjualanDetailMapper.toEntity(dto);
        TbPenjualanDetailEntity created = service.create(entity);
        return Response.ok(ApiResponse.success("Penjualan Detail created", penjualanDetailMapper.toDto(created))).build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") String id, PenjualanDetailDto dto) {
        TbPenjualanDetailEntity entity = penjualanDetailMapper.toEntity(dto);
        TbPenjualanDetailEntity updated = service.update(Long.valueOf(id), entity);
        if (updated == null) {
            return Response.ok(ApiResponse.error("Penjualan Detail not found")).build();
        }
        return Response.ok(ApiResponse.success("Penjualan Detail updated", penjualanDetailMapper.toDto(updated))).build();
    }

    @jakarta.ws.rs.DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") String id) {
        service.delete(Long.valueOf(id));
        return Response.ok(ApiResponse.success("Penjualan Detail deleted")).build();
    }
}
