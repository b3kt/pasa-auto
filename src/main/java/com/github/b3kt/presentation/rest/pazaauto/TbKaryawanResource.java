package com.github.b3kt.presentation.rest.pazaauto;

import java.util.List;

import com.github.b3kt.application.dto.ApiResponse;
import com.github.b3kt.application.service.pazaauto.AbstractCrudService;
import com.github.b3kt.application.service.pazaauto.TbKaryawanService;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbKaryawanEntity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@RequestScoped
@Path("/api/pazaauto/karyawan")
public class TbKaryawanResource extends AbstractCrudResource<TbKaryawanEntity, Long> {

    @Inject
    TbKaryawanService service;

    @Override
    protected AbstractCrudService<TbKaryawanEntity, Long> getService() {
        return service;
    }

    @Override
    protected Long parseId(String id) {
        return Long.valueOf(id);
    }

    @Override
    protected String getEntityName() {
        return "Karyawan";
    }

    @GET
    @Path("/unregistered")
    public Response listUnregistered() {
        List<TbKaryawanEntity> items = service.findAllUnregistered();
        return Response.ok(ApiResponse.success(items)).build();
    }
}
