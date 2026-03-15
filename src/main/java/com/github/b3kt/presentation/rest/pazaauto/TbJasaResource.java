package com.github.b3kt.presentation.rest.pazaauto;

import com.github.b3kt.application.service.pazaauto.AbstractCrudService;
import com.github.b3kt.application.service.pazaauto.TbJasaService;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbJasaEntity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.Path;
import lombok.RequiredArgsConstructor;

@RequestScoped
@Path("/api/pazaauto/jasa")
@RequiredArgsConstructor
public class TbJasaResource extends AbstractCrudResource<TbJasaEntity, Long> {

    private final TbJasaService service;

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
