package com.github.b3kt.presentation.rest.pazaauto;

import com.github.b3kt.application.dto.ApiResponse;
import com.github.b3kt.application.service.pazaauto.AbstractCrudService;
import com.github.b3kt.application.service.pazaauto.TbPelangganService;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPelangganEntity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

@RequestScoped
@Path("/api/pazaauto/pelanggan")
public class TbPelangganResource extends AbstractCrudResource<TbPelangganEntity, Long> {

    @Inject
    TbPelangganService service;

    @Override
    protected AbstractCrudService<TbPelangganEntity, Long> getService() {
        return service;
    }

    @Override
    protected Long parseId(String id) {
        return Long.valueOf(id);
    }

    @Override
    protected String getEntityName() {
        return "Pelanggan";
    }

    @GET
    @Path("/by-nopol/{nopol}")
    public Response findByNopol(@PathParam("nopol") String nopol) {
        TbPelangganEntity pelanggan = service.findByNopol(nopol);
        if (pelanggan == null) {
            return Response.ok(ApiResponse.error("Pelanggan not found for nopol: " + nopol)).build();
        }
        return Response.ok(ApiResponse.success(pelanggan)).build();
    }

    @PUT
    @Path("/by-nopol/{nopol}")
    public Response updateByNopol(@PathParam("nopol") String nopol, TbPelangganEntity pelangganData) {
        TbPelangganEntity updated = service.patchByNopol(nopol, pelangganData);
        if (updated == null) {
            return Response.ok(ApiResponse.error("Pelanggan not found for nopol: " + nopol)).build();
        }
        return Response.ok(ApiResponse.success("Pelanggan updated", updated)).build();
    }
}
