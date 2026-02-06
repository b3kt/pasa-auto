package com.github.b3kt.presentation.rest.pazaauto;

import com.github.b3kt.application.dto.ApiResponse;
import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.application.service.pazaauto.AbstractCrudService;
import com.github.b3kt.application.service.pazaauto.TbPembelianService;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPembelianEntity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@RequestScoped
@Path("/api/pazaauto/pembelian")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TbPembelianResource extends AbstractCrudResource<TbPembelianEntity, Long> {

    @Inject
    TbPembelianService service;

    @Override
    protected AbstractCrudService<TbPembelianEntity, Long> getService() {
        return service;
    }

    @Override
    protected Long parseId(String id) {
        return Long.parseLong(id);
    }

    @Override
    protected String getEntityName() {
        return "Pembelian";
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
            @QueryParam("jenisPembelian") String jenisPembelian,
            @QueryParam("kategoriOperasional") String kategoriOperasional,
            @QueryParam("startDate") String startDate,
            @QueryParam("endDate") String endDate) {

        PageRequest pageRequest = new PageRequest(page, rowsPerPage);
        pageRequest.setSortBy(sortBy);
        pageRequest.setDescending(descending);
        pageRequest.setSearch(search);
        pageRequest.setStatusFilter(statusFilter);
        pageRequest.setFilterToday(filterToday);
        pageRequest.setJenisPembelianFilter(jenisPembelian);
        pageRequest.setKategoriOperasionalFilter(kategoriOperasional);
        pageRequest.setStartDate(startDate);
        pageRequest.setEndDate(endDate);

        PageResponse<TbPembelianEntity> pageResponse = getService().findPaginated(pageRequest);
        return Response.ok(ApiResponse.success(pageResponse)).build();
    }

    @Override
    @POST
    public Response create(TbPembelianEntity entity) {
        // This won't be used, but kept for compatibility
        TbPembelianEntity created = service.create(entity);
        return Response.ok(ApiResponse.success(getEntityName() + " created", created)).build();
    }

    @POST
    @Path("/with-details")
    public Response createWithDetails(com.github.b3kt.application.dto.PembelianWithDetailsRequest request) {
        TbPembelianEntity created = service.createWithDetails(request.getPembelian(), request.getDetails());
        return Response.ok(ApiResponse.success(getEntityName() + " created with details", created)).build();
    }

    @Override
    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") String id, TbPembelianEntity entity) {
        // This won't be used, but kept for compatibility
        TbPembelianEntity updated = service.update(parseId(id), entity);
        return Response.ok(ApiResponse.success(getEntityName() + " updated", updated)).build();
    }

    @PUT
    @Path("/{id}/with-details")
    public Response updateWithDetails(@PathParam("id") String id,
            com.github.b3kt.application.dto.PembelianWithDetailsRequest request) {
        TbPembelianEntity updated = service.updateWithDetails(parseId(id), request.getPembelian(),
                request.getDetails());
        return Response.ok(ApiResponse.success(getEntityName() + " updated with details", updated)).build();
    }

    @GET
    @Path("/generate-no")
    public Response generateNoPembelian(@QueryParam("jenisPembelian") String jenisPembelian) {
         try {
             String noPembelian = service.generateNoPembelian(jenisPembelian);
             return Response.ok(ApiResponse.success(noPembelian)).build();
         } catch (Exception e) {
             return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                     .entity(ApiResponse.error("Failed to generate no pembelian: " + e.getMessage())).build();
         }
    }
}
