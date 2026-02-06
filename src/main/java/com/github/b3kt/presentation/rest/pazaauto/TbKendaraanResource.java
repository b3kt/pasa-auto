package com.github.b3kt.presentation.rest.pazaauto;

import com.github.b3kt.application.dto.ApiResponse;
import com.github.b3kt.application.service.pazaauto.AbstractCrudService;
import com.github.b3kt.application.service.pazaauto.TbKendaraanService;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbKendaraanEntity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import java.util.List;

@RequestScoped
@Path("/api/pazaauto/kendaraan")
public class TbKendaraanResource extends AbstractCrudResource<TbKendaraanEntity, Long> {

    @Inject
    TbKendaraanService service;

    @Override
    protected AbstractCrudService<TbKendaraanEntity, Long> getService() {
        return service;
    }

    @Override
    protected Long parseId(String id) {
        return Long.valueOf(id);
    }

    @Override
    protected String getEntityName() {
        return "Kendaraan";
    }

    @GET
    @Path("/merk/distinct")
    public Response getDistinctMerks() {
        List<String> merks = service.findDistinctMerks();
        return Response.ok(ApiResponse.success(merks)).build();
    }

    @GET
    @Path("/jenis/distinct")
    public Response getDistinctJenis() {
        List<String> jenis = service.findDistinctJenis();
        return Response.ok(ApiResponse.success(jenis)).build();
    }

    @GET
    @Path("/jenis/by-merk")
    public Response getDistinctJenisByMerk(@QueryParam("merk") String merk) {
        List<String> jenis;
        if (merk == null || merk.trim().isEmpty()) {
             jenis = service.findDistinctJenis();
        } else {
             jenis = service.findDistinctJenisByMerk(merk);
        }
        return Response.ok(ApiResponse.success(jenis)).build();
    }
}
