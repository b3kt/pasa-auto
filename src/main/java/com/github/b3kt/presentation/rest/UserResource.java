package com.github.b3kt.presentation.rest;

import com.github.b3kt.application.service.UserService;
import com.github.b3kt.application.service.pazaauto.AbstractCrudService;
import com.github.b3kt.infrastructure.persistence.entity.UserEntity;
import com.github.b3kt.presentation.rest.pazaauto.AbstractCrudResource;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;

@RequestScoped
@Path("/api/users")
public class UserResource extends AbstractCrudResource<UserEntity, Long> {

    @Inject
    UserService service;

    @Override
    protected AbstractCrudService<UserEntity, Long> getService() {
        return service;
    }

    @Override
    protected Long parseId(String id) {
        return Long.valueOf(id);
    }

    @Override
    protected String getEntityName() {
        return "User";
    }
}
