package com.github.b3kt.presentation.rest;

import com.github.b3kt.application.service.SystemParameterService;
import com.github.b3kt.application.service.pazaauto.AbstractCrudService;
import com.github.b3kt.infrastructure.persistence.entity.SystemParameterEntity;
import com.github.b3kt.presentation.rest.pazaauto.AbstractCrudResource;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;

@RequestScoped
@Path("/api/system-parameters")
public class SystemParameterResource extends AbstractCrudResource<SystemParameterEntity, Long> {

    @Inject
    SystemParameterService service;

    @Override
    protected AbstractCrudService<SystemParameterEntity, Long> getService() {
        return service;
    }

    @Override
    protected Long parseId(String id) {
        return Long.valueOf(id);
    }

    @Override
    protected String getEntityName() {
        return "System Parameter";
    }
}
