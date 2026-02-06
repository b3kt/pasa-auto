package com.github.b3kt.presentation.rest;

import com.github.b3kt.application.dto.ApiResponse;
import com.github.b3kt.application.dto.UpdateRoleUsersRequest;
import com.github.b3kt.application.service.RoleService;
import com.github.b3kt.application.service.pazaauto.AbstractCrudService;
import com.github.b3kt.infrastructure.persistence.entity.RoleEntity;
import com.github.b3kt.infrastructure.persistence.entity.UserEntity;
import com.github.b3kt.presentation.rest.pazaauto.AbstractCrudResource;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@RequestScoped
@Path("/api/roles")
public class RoleResource extends AbstractCrudResource<RoleEntity, Long> {

    @Inject
    RoleService service;

    @Override
    protected AbstractCrudService<RoleEntity, Long> getService() {
        return service;
    }

    @Override
    protected Long parseId(String id) {
        return Long.valueOf(id);
    }

    @Override
    protected String getEntityName() {
        return "Role";
    }

    /**
     * Get all users assigned to a specific role.
     */
    @GET
    @Path("/{id}/users")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRoleUsers(@PathParam("id") Long id) {
        List<UserEntity> users = service.getUsersByRoleId(id);
        return Response.ok(ApiResponse.success(users)).build();
    }

    /**
     * Update the list of users assigned to a role.
     */
    @PUT
    @Path("/{id}/users")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateRoleUsers(@PathParam("id") Long id, UpdateRoleUsersRequest request) {
        service.updateRoleUsers(id, request.getUserIds());
        return Response.ok(ApiResponse.success("Role users updated successfully", null)).build();
    }
}
