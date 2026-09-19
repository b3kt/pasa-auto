package com.github.b3kt.presentation.rest.pazaauto;

import com.github.b3kt.infrastructure.security.Roles;
import jakarta.annotation.security.RolesAllowed;
import com.github.b3kt.application.dto.ApiResponse;
import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.application.dto.pazaauto.MerkKendaraanDto;
import com.github.b3kt.application.mapper.pazaauto.MerkKendaraanMapper;
import com.github.b3kt.application.service.pazaauto.TbMerkKendaraanService;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbMerkKendaraanEntity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

@RequestScoped
@Path("/api/pazaauto/merk-kendaraan")
@RolesAllowed({Roles.ADMIN, Roles.OWNER})
public class TbMerkKendaraanResource {

    @Inject
    TbMerkKendaraanService service;

    @Inject
    MerkKendaraanMapper mapper;

    @GET
    public Response findAll() {
        return Response.ok(ApiResponse.success(mapper.toDtoList(service.findAll()))).build();
    }

    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") String id) {
        TbMerkKendaraanEntity entity = service.findById(Long.valueOf(id));
        return Response.ok(ApiResponse.success(mapper.toDto(entity))).build();
    }

    @GET
    @Path("/paginated")
    public Response listPaginated(
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("rowsPerPage") @DefaultValue("10") int rowsPerPage,
            @QueryParam("sortBy") String sortBy,
            @QueryParam("descending") @DefaultValue("false") boolean descending,
            @QueryParam("search") String search) {

        PageRequest pageRequest = new PageRequest(page, rowsPerPage);
        pageRequest.setSortBy(sortBy);
        pageRequest.setDescending(descending);
        pageRequest.setSearch(search);

        PageResponse<TbMerkKendaraanEntity> pageResponse = service.findPaginated(pageRequest);
        return Response.ok(ApiResponse.success(
                new PageResponse<>(
                        mapper.toDtoList(pageResponse.getRows()),
                        pageResponse.getCurrentPage(),
                        pageResponse.getRowsPerPage(),
                        pageResponse.getTotalRows()))).build();
    }

    @POST
    public Response create(MerkKendaraanDto dto) {
        TbMerkKendaraanEntity created = service.create(mapper.toEntity(dto));
        return Response.ok(ApiResponse.success("Merk kendaraan created", mapper.toDto(created))).build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") String id, MerkKendaraanDto dto) {
        TbMerkKendaraanEntity updated = service.update(Long.valueOf(id), mapper.toEntity(dto));
        return Response.ok(ApiResponse.success("Merk kendaraan updated", mapper.toDto(updated))).build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") String id) {
        service.delete(Long.valueOf(id));
        return Response.ok(ApiResponse.success("Merk kendaraan deleted")).build();
    }
}
