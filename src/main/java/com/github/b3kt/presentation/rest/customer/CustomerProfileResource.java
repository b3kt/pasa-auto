package com.github.b3kt.presentation.rest.customer;

import com.github.b3kt.application.dto.ApiResponse;
import com.github.b3kt.application.dto.customer.UpdateProfileRequest;
import com.github.b3kt.application.service.customer.CustomerProfileService;
import com.github.b3kt.application.service.pazaauto.KendaraanOwnershipService;
import com.github.b3kt.infrastructure.security.CustomerPrincipal;
import com.github.b3kt.infrastructure.security.Roles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** A customer's own profile and vehicles - always scoped by the caller's own linked pelangganId. */
@Path("/api/customer")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed(Roles.CUSTOMER)
@RequestScoped
public class CustomerProfileResource {

    @Inject
    CustomerProfileService profileService;

    @Inject
    KendaraanOwnershipService kendaraanOwnershipService;

    @Inject
    CustomerPrincipal principal;

    @GET
    @Path("/profile")
    public Response profile() {
        return Response.ok(ApiResponse.success(profileService.getProfile(principal.customerAccountId()))).build();
    }

    @PUT
    @Path("/profile")
    public Response updateProfile(@Valid UpdateProfileRequest request) {
        try {
            profileService.updateProfile(principal.pelangganIdOrThrow(), request);
            return Response.ok(ApiResponse.success("Profile updated",
                    profileService.getProfile(principal.customerAccountId()))).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(ApiResponse.error(e.getMessage())).build();
        }
    }

    @GET
    @Path("/vehicles")
    public Response vehicles() {
        return Response.ok(ApiResponse.success(
                kendaraanOwnershipService.listCurrentVehiclesByPelanggan(principal.pelangganIdOrThrow()))).build();
    }
}
