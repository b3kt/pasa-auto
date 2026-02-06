package com.github.b3kt.presentation.rest.pazaauto;

import com.github.b3kt.application.dto.ApiResponse;
import com.github.b3kt.application.service.pazaauto.AbstractCrudService;
import com.github.b3kt.application.service.pazaauto.TbPenjualanDetailService;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPenjualanDetailEntity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@RequestScoped
@Path("/api/pazaauto/penjualan-detail")
public class TbPenjualanDetailResource extends AbstractCrudResource<TbPenjualanDetailEntity, Long> {

    @Inject
    TbPenjualanDetailService service;

    @Override
    protected AbstractCrudService<TbPenjualanDetailEntity, Long> getService() {
        return service;
    }

    @Override
    protected Long parseId(String id) {
        return Long.parseLong(id);
    }

    @Override
    protected String getEntityName() {
        return "Penjualan Detail";
    }

    @Override
    public Response create(TbPenjualanDetailEntity entity) {
        TbPenjualanDetailEntity created = getService().create(entity);
        return Response.ok(ApiResponse.success(getEntityName() + " created", created)).build();
    }

}
