package com.github.b3kt.presentation.rest.customer;

import com.github.b3kt.application.dto.ApiResponse;
import com.github.b3kt.application.dto.customer.CustomerSpkDto;
import com.github.b3kt.application.service.customer.CustomerSpkQueryService;
import com.github.b3kt.infrastructure.security.CustomerPrincipal;
import com.github.b3kt.infrastructure.security.Roles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Read-only, status-only view of the caller's own work orders. No pricing/parts fields exist on
 * {@link CustomerSpkDto} - that boundary is enforced by the DTO shape, not by filtering here.
 */
@Path("/api/customer/spk")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed(Roles.CUSTOMER)
@RequestScoped
public class CustomerSpkResource {

    @Inject
    CustomerSpkQueryService spkQueryService;

    @Inject
    CustomerPrincipal principal;

    @GET
    public Response list() {
        return Response.ok(ApiResponse.success(spkQueryService.listForPelanggan(principal.pelangganIdOrThrow()))).build();
    }

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") Long id) {
        CustomerSpkDto dto = spkQueryService.getForPelanggan(id, principal.pelangganIdOrThrow());
        if (dto == null) {
            // Deliberately the same response whether the id doesn't exist or belongs to someone else.
            return Response.status(Response.Status.NOT_FOUND).entity(ApiResponse.error("Not found")).build();
        }
        return Response.ok(ApiResponse.success(dto)).build();
    }
}
