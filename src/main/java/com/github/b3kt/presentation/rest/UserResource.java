package com.github.b3kt.presentation.rest;

import com.github.b3kt.infrastructure.security.Roles;
import com.github.b3kt.application.service.UserService;
import com.github.b3kt.application.service.pazaauto.AbstractCrudService;
import com.github.b3kt.infrastructure.persistence.entity.UserEntity;
import com.github.b3kt.presentation.rest.pazaauto.AbstractCrudResource;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import com.github.b3kt.application.dto.ApiResponse;

@RequestScoped
@Path("/api/users")
@RolesAllowed(Roles.OWNER)
public class UserResource extends AbstractCrudResource<UserEntity, Long> {

    @Inject
    UserService service;

    /**
     * Accounts created by a Google sign-in that are waiting for an Owner.
     *
     * <p>Declared here rather than on the base class on purpose: inherited endpoints are checked
     * against {@code AbstractCrudResource}'s annotation, so a method added there would not pick up
     * this class's role gate.
     */
    @GET
    @Path("/pending")
    public Response listPending() {
        return Response.ok(ApiResponse.success(service.findPending())).build();
    }

    @POST
    @Path("/{id}/approve")
    public Response approve(@PathParam("id") String id) {
        return Response.ok(ApiResponse.success("User approved", service.approve(parseId(id)))).build();
    }

    @POST
    @Path("/{id}/reject")
    public Response reject(@PathParam("id") String id) {
        return Response.ok(ApiResponse.success("User rejected", service.reject(parseId(id)))).build();
    }

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
