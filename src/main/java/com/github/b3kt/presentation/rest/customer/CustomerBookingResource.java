package com.github.b3kt.presentation.rest.customer;

import com.github.b3kt.application.dto.ApiResponse;
import com.github.b3kt.application.dto.customer.CreateBookingRequest;
import com.github.b3kt.application.dto.customer.CustomerBookingDto;
import com.github.b3kt.application.service.customer.CustomerBookingService;
import com.github.b3kt.infrastructure.security.CustomerPrincipal;
import com.github.b3kt.infrastructure.security.Roles;
import com.github.b3kt.presentation.exception.RestErrorLogger;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/customer/bookings")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed(Roles.CUSTOMER)
@RequestScoped
public class CustomerBookingResource {

    @Inject
    CustomerBookingService bookingService;

    @Inject
    CustomerPrincipal principal;

    @POST
    public Response create(@Valid CreateBookingRequest request) {
        Long pelangganId = principal.pelangganIdOrThrow();
        CustomerBookingDto created = bookingService.create(principal.customerAccountId(), pelangganId, request);
        return Response.ok(ApiResponse.success("Booking requested", created)).build();
    }

    @GET
    public Response list() {
        return Response.ok(ApiResponse.success(bookingService.listForPelanggan(principal.pelangganIdOrThrow()))).build();
    }

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") Long id) {
        CustomerBookingDto dto = bookingService.getForPelanggan(id, principal.pelangganIdOrThrow());
        if (dto == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(ApiResponse.error("Not found")).build();
        }
        return Response.ok(ApiResponse.success(dto)).build();
    }

    @POST
    @Path("/{id}/cancel")
    public Response cancel(@PathParam("id") Long id) {
        try {
            bookingService.cancel(id, principal.pelangganIdOrThrow());
            return Response.ok(ApiResponse.success("Booking cancelled", null)).build();
        } catch (IllegalArgumentException e) {
            RestErrorLogger.log(Response.Status.BAD_REQUEST, e);
            return Response.status(Response.Status.BAD_REQUEST).entity(ApiResponse.error(e.getMessage())).build();
        }
    }
}
