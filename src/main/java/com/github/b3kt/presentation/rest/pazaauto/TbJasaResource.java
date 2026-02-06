package com.github.b3kt.presentation.rest.pazaauto;

import com.github.b3kt.application.service.pazaauto.AbstractCrudService;
import com.github.b3kt.application.service.pazaauto.TbJasaService;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbJasaEntity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;

@RequestScoped
@Path("/api/pazaauto/jasa")
public class TbJasaResource extends AbstractCrudResource<TbJasaEntity, Long> {

    @Inject
    TbJasaService service;

    @Override
    protected AbstractCrudService<TbJasaEntity, Long> getService() {
        return service;
    }

    @Override
    protected Long parseId(String id) {
        return Long.valueOf(id);
    }

    @Override
    protected String getEntityName() {
        return "Jasa";
    }
}
