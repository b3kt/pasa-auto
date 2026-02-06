package com.github.b3kt.presentation.rest;

import com.github.b3kt.application.service.PermissionService;
import com.github.b3kt.application.service.pazaauto.AbstractCrudService;
import com.github.b3kt.infrastructure.persistence.entity.PermissionEntity;
import com.github.b3kt.presentation.rest.pazaauto.AbstractCrudResource;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;

@RequestScoped
@Path("/api/permissions")
public class PermissionResource extends AbstractCrudResource<PermissionEntity, Long> {

    @Inject
    PermissionService service;

    @Override
    protected AbstractCrudService<PermissionEntity, Long> getService() {
        return service;
    }

    @Override
    protected Long parseId(String id) {
        return Long.valueOf(id);
    }

    @Override
    protected String getEntityName() {
        return "Permission";
    }
}
