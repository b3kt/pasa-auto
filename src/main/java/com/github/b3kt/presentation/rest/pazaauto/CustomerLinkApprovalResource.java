package com.github.b3kt.presentation.rest.pazaauto;

import com.github.b3kt.application.dto.ApiResponse;
import com.github.b3kt.application.dto.customer.ReviewRequest;
import com.github.b3kt.application.service.customer.CustomerLinkService;
import com.github.b3kt.infrastructure.security.Roles;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** Staff review queue for customer link requests that conflict with an existing claim. */
@Path("/api/pazaauto/customer-link-requests")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({Roles.ADMIN, Roles.OWNER})
@RequestScoped
public class CustomerLinkApprovalResource {

    @Inject
    CustomerLinkService linkService;

    @Inject
    SecurityIdentity identity;

    @GET
    public Response list(@QueryParam("status") @DefaultValue("PENDING") String status) {
        return Response.ok(ApiResponse.success(linkService.listRequests(status))).build();
    }

    @POST
    @Path("/{id}/approve")
    public Response approve(@PathParam("id") Long id) {
        try {
            linkService.approve(id, identity.getPrincipal().getName());
            return Response.ok(ApiResponse.success("Link request approved", null)).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(ApiResponse.error(e.getMessage())).build();
        }
    }

    @POST
    @Path("/{id}/reject")
    public Response reject(@PathParam("id") Long id, ReviewRequest request) {
        try {
            linkService.reject(id, identity.getPrincipal().getName(), request != null ? request.getNote() : null);
            return Response.ok(ApiResponse.success("Link request rejected", null)).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(ApiResponse.error(e.getMessage())).build();
        }
    }
}
