package com.github.b3kt.presentation.rest.customer;

import com.github.b3kt.application.dto.ApiResponse;
import com.github.b3kt.application.dto.customer.UpdateProfileRequest;
import com.github.b3kt.application.dto.customer.VehicleClaimRequest;
import com.github.b3kt.application.dto.pazaauto.KendaraanAttachRequest;
import com.github.b3kt.application.service.customer.CustomerProfileService;
import com.github.b3kt.application.service.customer.VehicleClaimService;
import com.github.b3kt.application.service.pazaauto.KendaraanOwnershipService;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbKendaraanRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbMerkKendaraanRepository;
import com.github.b3kt.infrastructure.security.CustomerPrincipal;
import com.github.b3kt.infrastructure.security.Roles;
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
    VehicleClaimService vehicleClaimService;

    @Inject
    TbMerkKendaraanRepository merkRepository;

    @Inject
    TbKendaraanRepository kendaraanRepository;

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
    public Response registerVehicle(@Valid KendaraanAttachRequest request) {
        try {
            kendaraanOwnershipService.attach(
                    principal.pelangganIdOrThrow(),
                    request.getNopol(),
                    request.getMerk(),
                    request.getJenis(),
                    request.getTanggalMulai(),
                    request.getKeterangan()
            );
            return Response.ok(ApiResponse.success("Vehicle registered successfully",
                    kendaraanOwnershipService.listCurrentVehiclesByPelanggan(principal.pelangganIdOrThrow()))).build();
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
        try {
            return Response.ok(ApiResponse.success(
                    kendaraanOwnershipService.checkNopol(principal.pelangganIdOrThrow(), nopol))).build();
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
}
