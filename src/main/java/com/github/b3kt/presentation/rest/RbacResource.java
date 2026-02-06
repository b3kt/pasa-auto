package com.github.b3kt.presentation.rest;

import com.github.b3kt.application.dto.ApiResponse;
import com.github.b3kt.application.dto.CreatePermissionRequest;
import com.github.b3kt.application.dto.CreateRoleRequest;
import com.github.b3kt.application.properties.RbacProperties;
import com.github.b3kt.application.service.RbacService;
import com.github.b3kt.domain.model.Permission;
import com.github.b3kt.domain.model.Role;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.Set;

/**
 * REST controller for RBAC (Role-Based Access Control) endpoints.
 */
@Path("/api/rbac")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "RBAC", description = "Role-Based Access Control management endpoints")
public class RbacResource {

    @Inject
    RbacService rbacService;

    @Inject
    RbacProperties rbacProperties;

    // Role endpoints
    @POST
    @Path("/roles")
    @RolesAllowed("admin")
    @Operation(
        summary = "Create a new role",
        description = "Create a new role in the RBAC system. Requires admin role."
    )
    @SecurityRequirement(name = "bearerAuth")
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Role created successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = ApiResponse.class)
            )
        ),
        @APIResponse(
            responseCode = "400",
            description = "Validation error or role already exists",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = ApiResponse.class)
            )
        ),
        @APIResponse(
            responseCode = "403",
            description = "RBAC module is not enabled"
        )
    })
    public Response createRole(@Valid CreateRoleRequest request) {
        try {
            checkRbacEnabled();
            Role role = rbacService.createRole(request.getName(), request.getDescription());
            return Response.ok(ApiResponse.success("Role created successfully", role)).build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(ApiResponse.<Role>error(e.getMessage()))
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.<Role>error(e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/roles")
    @RolesAllowed("admin")
    @Operation(
        summary = "Get all roles",
        description = "Retrieve all roles in the RBAC system. Requires admin role."
    )
    @SecurityRequirement(name = "bearerAuth")
    public Response getAllRoles() {
        try {
            checkRbacEnabled();
            List<Role> roles = rbacService.getAllRoles();
            return Response.ok(ApiResponse.success(roles)).build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(ApiResponse.<List<Role>>error(e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/roles/{id}")
    @RolesAllowed("admin")
    @Operation(
        summary = "Get role by ID",
        description = "Retrieve a specific role by its ID. Requires admin role."
    )
    @SecurityRequirement(name = "bearerAuth")
    public Response getRoleById(@PathParam("id") Long id) {
        try {
            checkRbacEnabled();
            Role role = rbacService.getRoleById(id);
            return Response.ok(ApiResponse.success(role)).build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(ApiResponse.<Role>error(e.getMessage()))
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ApiResponse.<Role>error(e.getMessage()))
                    .build();
        }
    }

    @PUT
    @Path("/roles/{id}")
    @RolesAllowed("admin")
    @Operation(
        summary = "Update a role",
        description = "Update an existing role. Requires admin role."
    )
    @SecurityRequirement(name = "bearerAuth")
    public Response updateRole(@PathParam("id") Long id, @Valid CreateRoleRequest request) {
        try {
            checkRbacEnabled();
            Role role = rbacService.updateRole(id, request.getName(), request.getDescription());
            return Response.ok(ApiResponse.success("Role updated successfully", role)).build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(ApiResponse.<Role>error(e.getMessage()))
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.<Role>error(e.getMessage()))
                    .build();
        }
    }

    @DELETE
    @Path("/roles/{id}")
    @RolesAllowed("admin")
    @Operation(
        summary = "Delete a role",
        description = "Delete a role from the RBAC system. Requires admin role."
    )
    @SecurityRequirement(name = "bearerAuth")
    public Response deleteRole(@PathParam("id") Long id) {
        try {
            checkRbacEnabled();
            rbacService.deleteRole(id);
            return Response.ok(ApiResponse.success("Role deleted successfully", null)).build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(ApiResponse.error(e.getMessage()))
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ApiResponse.error(e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/roles/{id}/activate")
    @RolesAllowed("admin")
    @Operation(
        summary = "Activate a role",
        description = "Activate a deactivated role. Requires admin role."
    )
    @SecurityRequirement(name = "bearerAuth")
    public Response activateRole(@PathParam("id") Long id) {
        try {
            checkRbacEnabled();
            rbacService.activateRole(id);
            return Response.ok(ApiResponse.success("Role activated successfully", null)).build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(ApiResponse.error(e.getMessage()))
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ApiResponse.error(e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/roles/{id}/deactivate")
    @RolesAllowed("admin")
    @Operation(
        summary = "Deactivate a role",
        description = "Deactivate a role. Requires admin role."
    )
    @SecurityRequirement(name = "bearerAuth")
    public Response deactivateRole(@PathParam("id") Long id) {
        try {
            checkRbacEnabled();
            rbacService.deactivateRole(id);
            return Response.ok(ApiResponse.success("Role deactivated successfully", null)).build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(ApiResponse.error(e.getMessage()))
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ApiResponse.error(e.getMessage()))
                    .build();
        }
    }

    // Permission endpoints
    @POST
    @Path("/permissions")
    @RolesAllowed("admin")
    @Operation(
        summary = "Create a new permission",
        description = "Create a new permission in the RBAC system. Requires admin role."
    )
    @SecurityRequirement(name = "bearerAuth")
    public Response createPermission(@Valid CreatePermissionRequest request) {
        try {
            checkRbacEnabled();
            Permission permission = rbacService.createPermission(
                request.getName(),
                request.getDescription(),
                request.getResource(),
                request.getAction()
            );
            return Response.ok(ApiResponse.success("Permission created successfully", permission)).build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(ApiResponse.<Permission>error(e.getMessage()))
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.<Permission>error(e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/permissions")
    @RolesAllowed("admin")
    @Operation(
        summary = "Get all permissions",
        description = "Retrieve all permissions in the RBAC system. Requires admin role."
    )
    @SecurityRequirement(name = "bearerAuth")
    public Response getAllPermissions() {
        try {
            checkRbacEnabled();
            List<Permission> permissions = rbacService.getAllPermissions();
            return Response.ok(ApiResponse.success(permissions)).build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(ApiResponse.<List<Permission>>error(e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/permissions/{id}")
    @RolesAllowed("admin")
    @Operation(
        summary = "Get permission by ID",
        description = "Retrieve a specific permission by its ID. Requires admin role."
    )
    @SecurityRequirement(name = "bearerAuth")
    public Response getPermissionById(@PathParam("id") Long id) {
        try {
            checkRbacEnabled();
            Permission permission = rbacService.getPermissionById(id);
            return Response.ok(ApiResponse.success(permission)).build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(ApiResponse.<Permission>error(e.getMessage()))
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ApiResponse.<Permission>error(e.getMessage()))
                    .build();
        }
    }

    // Role-Permission endpoints
    @POST
    @Path("/roles/{roleId}/permissions/{permissionId}")
    @RolesAllowed("admin")
    @Operation(
        summary = "Assign permission to role",
        description = "Assign a permission to a role. Requires admin role."
    )
    @SecurityRequirement(name = "bearerAuth")
    public Response assignPermissionToRole(
            @PathParam("roleId") Long roleId,
            @PathParam("permissionId") Long permissionId) {
        try {
            checkRbacEnabled();
            rbacService.assignPermissionToRole(roleId, permissionId);
            return Response.ok(ApiResponse.success("Permission assigned to role successfully", null)).build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(ApiResponse.error(e.getMessage()))
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error(e.getMessage()))
                    .build();
        }
    }

    @DELETE
    @Path("/roles/{roleId}/permissions/{permissionId}")
    @RolesAllowed("admin")
    @Operation(
        summary = "Remove permission from role",
        description = "Remove a permission from a role. Requires admin role."
    )
    @SecurityRequirement(name = "bearerAuth")
    public Response removePermissionFromRole(
            @PathParam("roleId") Long roleId,
            @PathParam("permissionId") Long permissionId) {
        try {
            checkRbacEnabled();
            rbacService.removePermissionFromRole(roleId, permissionId);
            return Response.ok(ApiResponse.success("Permission removed from role successfully", null)).build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(ApiResponse.error(e.getMessage()))
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error(e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/roles/{roleId}/permissions")
    @RolesAllowed("admin")
    @Operation(
        summary = "Get permissions for a role",
        description = "Retrieve all permissions assigned to a role. Requires admin role."
    )
    @SecurityRequirement(name = "bearerAuth")
    public Response getRolePermissions(@PathParam("roleId") Long roleId) {
        try {
            checkRbacEnabled();
            Set<Permission> permissions = rbacService.getRolePermissions(roleId);
            return Response.ok(ApiResponse.success(permissions)).build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(ApiResponse.<Set<Permission>>error(e.getMessage()))
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ApiResponse.<Set<Permission>>error(e.getMessage()))
                    .build();
        }
    }

    // User-Role endpoints
    @POST
    @Path("/users/{username}/roles/{roleId}")
    @RolesAllowed("admin")
    @Operation(
        summary = "Assign role to user",
        description = "Assign a role to a user. Requires admin role."
    )
    @SecurityRequirement(name = "bearerAuth")
    public Response assignRoleToUser(
            @PathParam("username") String username,
            @PathParam("roleId") Long roleId) {
        try {
            checkRbacEnabled();
            rbacService.assignRoleToUser(username, roleId);
            return Response.ok(ApiResponse.success("Role assigned to user successfully", null)).build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(ApiResponse.error(e.getMessage()))
                    .build();
        } catch (IllegalArgumentException | com.github.b3kt.domain.exception.UserNotFoundException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error(e.getMessage()))
                    .build();
        }
    }

    @DELETE
    @Path("/users/{username}/roles/{roleId}")
    @RolesAllowed("admin")
    @Operation(
        summary = "Remove role from user",
        description = "Remove a role from a user. Requires admin role."
    )
    @SecurityRequirement(name = "bearerAuth")
    public Response removeRoleFromUser(
            @PathParam("username") String username,
            @PathParam("roleId") Long roleId) {
        try {
            checkRbacEnabled();
            rbacService.removeRoleFromUser(username, roleId);
            return Response.ok(ApiResponse.success("Role removed from user successfully", null)).build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(ApiResponse.error(e.getMessage()))
                    .build();
        } catch (IllegalArgumentException | com.github.b3kt.domain.exception.UserNotFoundException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error(e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/users/{username}/roles")
    @RolesAllowed("admin")
    @Operation(
        summary = "Get roles for a user",
        description = "Retrieve all roles assigned to a user. Requires admin role."
    )
    @SecurityRequirement(name = "bearerAuth")
    public Response getUserRoles(@PathParam("username") String username) {
        try {
            checkRbacEnabled();
            Set<Role> roles = rbacService.getUserRoles(username);
            return Response.ok(ApiResponse.success(roles)).build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(ApiResponse.<Set<Role>>error(e.getMessage()))
                    .build();
        } catch (com.github.b3kt.domain.exception.UserNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ApiResponse.<Set<Role>>error(e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/users/{username}/permissions")
    @RolesAllowed("admin")
    @Operation(
        summary = "Get permissions for a user",
        description = "Retrieve all permissions for a user (aggregated from all roles). Requires admin role."
    )
    @SecurityRequirement(name = "bearerAuth")
    public Response getUserPermissions(@PathParam("username") String username) {
        try {
            checkRbacEnabled();
            Set<Permission> permissions = rbacService.getUserPermissions(username);
            return Response.ok(ApiResponse.success(permissions)).build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(ApiResponse.<Set<Permission>>error(e.getMessage()))
                    .build();
        } catch (com.github.b3kt.domain.exception.UserNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ApiResponse.<Set<Permission>>error(e.getMessage()))
                    .build();
        }
    }

    private void checkRbacEnabled() {
        if (!rbacProperties.enabled()) {
            throw new IllegalStateException("RBAC module is not enabled. Set app.rbac.enabled=true to use RBAC features.");
        }
    }
}

