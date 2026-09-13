package com.github.b3kt.presentation.rest.pazaauto;

import com.github.b3kt.application.dto.ApiResponse;
import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.application.dto.pazaauto.PembelianDetailDto;
import com.github.b3kt.application.mapper.pazaauto.PembelianDetailMapper;
import com.github.b3kt.application.service.pazaauto.TbPembelianDetailService;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPembelianDetailEntity;
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
@Path("/api/pazaauto/pembelian-detail")
public class TbPembelianDetailResource {

    @Inject
    TbPembelianDetailService service;

    @Inject
    PembelianDetailMapper pembelianDetailMapper;

    @GET
    public Response findAll() {
        return Response.ok(ApiResponse.success(pembelianDetailMapper.toDtoList(service.findAll()))).build();
    }

    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") String id) {
        TbPembelianDetailEntity entity = service.findById(Long.valueOf(id));
        if (entity == null) {
            return Response.ok(ApiResponse.error("Pembelian Detail not found")).build();
        }
        return Response.ok(ApiResponse.success(pembelianDetailMapper.toDto(entity))).build();
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

        PageResponse<TbPembelianDetailEntity> pageResponse = service.findPaginated(pageRequest);
        return Response.ok(ApiResponse.success(
                new PageResponse<>(
                        pembelianDetailMapper.toDtoList(pageResponse.getRows()),
                        pageResponse.getCurrentPage(),
                        pageResponse.getRowsPerPage(),
                        pageResponse.getTotalRows()))).build();
    }

    @GET
    @Path("/by-pembelian/{pembelianId}")
    public Response getByPembelianId(@PathParam("pembelianId") String pembelianId) {
        Long id = Long.parseLong(pembelianId);
        List<TbPembelianDetailEntity> details = service.findByPembelianId(id);
        return Response.ok(ApiResponse.success(pembelianDetailMapper.toDtoList(details))).build();
    }

    @POST
    public Response create(PembelianDetailDto dto) {
        TbPembelianDetailEntity entity = pembelianDetailMapper.toEntity(dto);
        TbPembelianDetailEntity created = service.create(entity);
        return Response.ok(ApiResponse.success("Pembelian Detail created", pembelianDetailMapper.toDto(created))).build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") String id, PembelianDetailDto dto) {
        TbPembelianDetailEntity entity = pembelianDetailMapper.toEntity(dto);
        TbPembelianDetailEntity updated = service.update(Long.valueOf(id), entity);
        if (updated == null) {
            return Response.ok(ApiResponse.error("Pembelian Detail not found")).build();
        }
        return Response.ok(ApiResponse.success("Pembelian Detail updated", pembelianDetailMapper.toDto(updated))).build();
    }

    @jakarta.ws.rs.DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") String id) {
        service.delete(Long.valueOf(id));
        return Response.ok(ApiResponse.success("Pembelian Detail deleted")).build();
    }
}
