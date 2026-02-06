package com.github.b3kt.presentation.rest.pazaauto;

import com.github.b3kt.application.dto.ApiResponse;
import com.github.b3kt.application.service.pazaauto.AbstractCrudService;
import com.github.b3kt.application.service.pazaauto.TbPembelianDetailService;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPembelianDetailEntity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@RequestScoped
@Path("/api/pazaauto/pembelian-detail")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TbPembelianDetailResource extends AbstractCrudResource<TbPembelianDetailEntity, Long> {

    @Inject
    TbPembelianDetailService service;

    @Override
    protected AbstractCrudService<TbPembelianDetailEntity, Long> getService() {
        return service;
    }

    @Override
    protected Long parseId(String id) {
        return Long.parseLong(id);
    }

    @Override
    protected String getEntityName() {
        return "Pembelian Detail";
    }

    @GET
    @Path("/by-pembelian/{pembelianId}")
    public Response getByPembelianId(@PathParam("pembelianId") String pembelianId) {
        Long id = Long.parseLong(pembelianId);
        List<TbPembelianDetailEntity> details = service.findByPembelianId(id);
        return Response.ok(ApiResponse.success(details)).build();
    }
}
