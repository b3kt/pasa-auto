package com.github.b3kt.presentation.rest.pazaauto;

import com.github.b3kt.application.dto.ApiResponse;
import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.application.dto.pazaauto.SpkDetailDto;
import com.github.b3kt.application.mapper.pazaauto.SpkDetailMapper;
import com.github.b3kt.application.service.pazaauto.TbSpkDetailService;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSpkDetailEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSpkDetailId;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.core.Response;

@RequestScoped
@Path("/api/pazaauto/spk-detail")
public class TbSpkDetailResource {

    @Inject
    TbSpkDetailService service;

    @Inject
    SpkDetailMapper spkDetailMapper;

    @GET
    public Response findAll() {
        return Response.ok(ApiResponse.success(spkDetailMapper.toDtoList(service.findAll()))).build();
    }

    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") String id) {
        TbSpkDetailId detailId = parseId(id);
        TbSpkDetailEntity entity = service.findById(detailId);
        if (entity == null) {
            return Response.ok(ApiResponse.error("SPK Detail not found")).build();
        }
        return Response.ok(ApiResponse.success(spkDetailMapper.toDto(entity))).build();
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

        PageResponse<TbSpkDetailEntity> pageResponse = service.findPaginated(pageRequest);
        return Response.ok(ApiResponse.success(
                new PageResponse<>(
                        spkDetailMapper.toDtoList(pageResponse.getRows()),
                        pageResponse.getCurrentPage(),
                        pageResponse.getRowsPerPage(),
                        pageResponse.getTotalRows()))).build();
    }

    @GET
    @Path("/by-spk/{noSpk}")
    public Response getBySpk(@PathParam("noSpk") String noSpk) {
        return Response.ok(ApiResponse.success(spkDetailMapper.toDtoList(service.findByNoSpk(noSpk)))).build();
    }

    @POST
    public Response create(SpkDetailDto dto) {
        TbSpkDetailEntity entity = spkDetailMapper.toEntity(dto);
        TbSpkDetailEntity created = service.create(entity);
        return Response.ok(ApiResponse.success("SPK Detail created", spkDetailMapper.toDto(created))).build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") String id, SpkDetailDto dto) {
        TbSpkDetailId detailId = parseId(id);
        TbSpkDetailEntity entity = spkDetailMapper.toEntity(dto);
        TbSpkDetailEntity updated = service.update(detailId, entity);
        if (updated == null) {
            return Response.ok(ApiResponse.error("SPK Detail not found")).build();
        }
        return Response.ok(ApiResponse.success("SPK Detail updated", spkDetailMapper.toDto(updated))).build();
    }

    @jakarta.ws.rs.DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") String id) {
        TbSpkDetailId detailId = parseId(id);
        service.delete(detailId);
        return Response.ok(ApiResponse.success("SPK Detail deleted")).build();
    }

    protected TbSpkDetailId parseId(String id) {
        String[] parts = id.split(":", 2);
        if (parts.length != 2) {
            throw new BadRequestException("Invalid identifier format. Use 'noSpk:namaJasa'.");
        }
        return new TbSpkDetailId(parts[0], parts[1]);
    }
}

