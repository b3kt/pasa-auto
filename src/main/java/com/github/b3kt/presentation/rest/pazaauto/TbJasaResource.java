package com.github.b3kt.presentation.rest.pazaauto;

import com.github.b3kt.application.dto.ApiResponse;
import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.application.dto.pazaauto.JasaDto;
import com.github.b3kt.application.mapper.pazaauto.JasaMapper;
import com.github.b3kt.application.service.pazaauto.TbJasaService;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbJasaEntity;
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
@Path("/api/pazaauto/jasa")
public class TbJasaResource {

    @Inject
    TbJasaService service;

    @Inject
    JasaMapper jasaMapper;

    @GET
    public Response findAll() {
        return Response.ok(ApiResponse.success(jasaMapper.toDtoList(service.findAll()))).build();
    }

    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") String id) {
        TbJasaEntity entity = service.findById(Long.valueOf(id));
        if (entity == null) {
            return Response.ok(ApiResponse.error("Jasa not found")).build();
        }
        return Response.ok(ApiResponse.success(jasaMapper.toDto(entity))).build();
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

        PageResponse<TbJasaEntity> pageResponse = service.findPaginated(pageRequest);
        return Response.ok(ApiResponse.success(
                new PageResponse<>(
                        jasaMapper.toDtoList(pageResponse.getRows()),
                        pageResponse.getCurrentPage(),
                        pageResponse.getRowsPerPage(),
                        pageResponse.getTotalRows()))).build();
    }

    @POST
    public Response create(JasaDto dto) {
        TbJasaEntity entity = jasaMapper.toEntity(dto);
        TbJasaEntity created = service.create(entity);
        return Response.ok(ApiResponse.success("Jasa created", jasaMapper.toDto(created))).build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") String id, JasaDto dto) {
        TbJasaEntity entity = jasaMapper.toEntity(dto);
        TbJasaEntity updated = service.update(Long.valueOf(id), entity);
        if (updated == null) {
            return Response.ok(ApiResponse.error("Jasa not found")).build();
        }
        return Response.ok(ApiResponse.success("Jasa updated", jasaMapper.toDto(updated))).build();
    }

    @jakarta.ws.rs.DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") String id) {
        service.delete(Long.valueOf(id));
        return Response.ok(ApiResponse.success("Jasa deleted")).build();
    }
}
