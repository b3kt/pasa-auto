package com.github.b3kt.presentation.rest.pazaauto;

import com.github.b3kt.application.dto.ApiResponse;
import com.github.b3kt.application.dto.pazaauto.PenjualanPrintDto;
import com.github.b3kt.application.service.pazaauto.*;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPenjualanEntity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;

@RequestScoped
@Path("/api/pazaauto/penjualan")
@RequiredArgsConstructor
public class TbPenjualanResource extends AbstractCrudResource<TbPenjualanEntity, String> {

    private final TbPenjualanService service;

    @Override
    protected AbstractCrudService<TbPenjualanEntity, String> getService() {
        return service;
    }

    @Override
    protected String parseId(String id) {
        return id;
    }

    @Override
    protected String getEntityName() {
        return "Penjualan";
    }

    @GET
    @Path("/{noPenjualan}/print")
    public Response print(@PathParam("noPenjualan") String noPenjualan) {
        PenjualanPrintDto dto = service.buildPrintDto(noPenjualan);
        if (dto == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(ApiResponse.success(dto)).build();
    }

    @Override
    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") String noPenjualan) {
        TbPenjualanEntity entity = service.findByNoPenjualan(noPenjualan);
        if (entity == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(ApiResponse.success(entity)).build();
    }

    @Override
    @POST
    public Response create(TbPenjualanEntity entity) {
        TbPenjualanEntity created = service.createWithNoSpkValidation(entity);
        return Response.ok(ApiResponse.success(getEntityName() + " created", created)).build();
    }

    @Override
    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") String id, TbPenjualanEntity entity) {
        TbPenjualanEntity updated = service.updateWithNoSpkValidation(entity);
        return Response.ok(ApiResponse.success(getEntityName() + " updated", updated)).build();
    }

    @DELETE
    @Path("/cancel-by-no-spk/{noSpk}")
    public Response cancelByNoSpk(@PathParam("noSpk") String noSpk) {
        service.cancelPenjualanBySpk(noSpk);
        return Response.ok(ApiResponse.success("Penjualan cancelled, SPK status reverted to OPEN")).build();
    }
}
