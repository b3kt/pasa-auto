package com.github.b3kt.presentation.rest.customer;

import com.github.b3kt.application.dto.ApiResponse;
import com.github.b3kt.application.dto.customer.CustomerVehicleRegisterRequest;
import com.github.b3kt.application.dto.customer.UpdateProfileRequest;
import com.github.b3kt.application.dto.customer.VehicleClaimRequest;
import com.github.b3kt.application.dto.pazaauto.KendaraanAttachRequest;
import com.github.b3kt.application.service.customer.CustomerProfileService;
import com.github.b3kt.application.helper.NopolNormalizer;
import com.github.b3kt.application.service.customer.CustomerVehicleService;
import com.github.b3kt.application.service.customer.NopolLookupRateLimiter;
import com.github.b3kt.application.service.customer.VehicleClaimService;
import com.github.b3kt.application.service.pazaauto.KendaraanOwnershipService;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbKendaraanRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbMerkKendaraanRepository;
import com.github.b3kt.infrastructure.security.CustomerPrincipal;
import com.github.b3kt.infrastructure.security.Roles;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.SocketAddress;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
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
    CustomerVehicleService customerVehicleService;

    @Inject
    KendaraanOwnershipService kendaraanOwnershipService;

    @Inject
    VehicleClaimService vehicleClaimService;

    @Inject
    TbMerkKendaraanRepository merkRepository;

    @Inject
    TbKendaraanRepository kendaraanRepository;

    @Inject
    CustomerPrincipal principal;

    @Inject
    NopolLookupRateLimiter lookupRateLimiter;

    @Context
    HttpServerRequest request;

    @GET
    @Path("/profile")
    public Response profile() {
        return Response.ok(ApiResponse.success(profileService.getProfile(principal.customerAccountId()))).build();
    }

    @PUT
    @Path("/profile")
    public Response updateProfile(@Valid UpdateProfileRequest request) {
        try {
            profileService.updateProfile(principal.customerAccountId(), request);
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

    @POST
    @Path("/vehicles")
    public Response registerVehicle(@Valid CustomerVehicleRegisterRequest request) {
        try {
            return Response.ok(ApiResponse.success("Vehicle registered successfully",
                    customerVehicleService.registerVehicle(principal.customerAccountId(), request))).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(ApiResponse.error(e.getMessage())).build();
        }
    }

    @PUT
    @Path("/vehicles/{nopol}")
    public Response updateVehicle(@PathParam("nopol") String nopol, @Valid KendaraanAttachRequest request) {
        try {
            kendaraanOwnershipService.updateVehicle(
                    principal.pelangganIdOrThrow(),
                    nopol,
                    request.getMerk(),
                    request.getJenis()
            );
            return Response.ok(ApiResponse.success("Vehicle updated successfully",
                    kendaraanOwnershipService.listCurrentVehiclesByPelanggan(principal.pelangganIdOrThrow()))).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(ApiResponse.error(e.getMessage())).build();
        }
    }

    @DELETE
    @Path("/vehicles/{nopol}")
    public Response deleteVehicle(@PathParam("nopol") String nopol) {
        try {
            kendaraanOwnershipService.deleteVehicle(
                    principal.pelangganIdOrThrow(),
                    nopol
            );
            return Response.ok(ApiResponse.success("Vehicle deleted successfully",
                    kendaraanOwnershipService.listCurrentVehiclesByPelanggan(principal.pelangganIdOrThrow()))).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(ApiResponse.error(e.getMessage())).build();
        }
    }

    @GET
    @Path("/vehicles/check-nopol")
    public Response checkNopol(@QueryParam("nopol") String nopol) {
        // A lookup reveals whether a plate is on record (ADR-0013): bound how many distinct plates one
        // account / one address can probe. Over the limit -> 429 with Retry-After (global mapper).
        String norm = NopolNormalizer.normalize(nopol);
        if (!norm.isEmpty()) {
            lookupRateLimiter.checkAndRecord(principal.customerAccountId(), clientIp(), norm);
        }
        try {
            return Response.ok(ApiResponse.success(
                    customerVehicleService.checkNopol(principal.customerAccountId(), nopol))).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(ApiResponse.error(e.getMessage())).build();
        }
    }

    @POST
    @Path("/vehicles/claim")
    public Response claimVehicle(@Valid VehicleClaimRequest request) {
        try {
            return Response.ok(ApiResponse.success("Vehicle claim received",
                    vehicleClaimService.requestClaim(principal.customerAccountId(), request.getNopol()))).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(ApiResponse.error(e.getMessage())).build();
        }
    }

    @GET
    @Path("/vehicles/merk")
    @PermitAll
    public Response listMerk() {
        return Response.ok(ApiResponse.success(merkRepository.findAllNama())).build();
    }

    @GET
    @Path("/vehicles/jenis")
    @PermitAll
    public Response listJenis(@QueryParam("merk") String merk) {
        if (merk != null && !merk.isBlank()) {
            return Response.ok(ApiResponse.success(kendaraanRepository.findDistinctJenisByMerk(merk))).build();
        }
        return Response.ok(ApiResponse.success(kendaraanRepository.findDistinctJenis())).build();
    }

    private String clientIp() {
        SocketAddress remoteAddress = request != null ? request.remoteAddress() : null;
        return remoteAddress != null ? remoteAddress.hostAddress() : null;
    }
}
