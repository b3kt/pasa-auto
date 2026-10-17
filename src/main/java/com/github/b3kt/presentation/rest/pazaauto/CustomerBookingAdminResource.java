package com.github.b3kt.presentation.rest.pazaauto;

import com.github.b3kt.application.dto.ApiResponse;
import com.github.b3kt.application.dto.customer.ConvertToSpkRequest;
import com.github.b3kt.application.dto.customer.ReviewRequest;
import com.github.b3kt.application.service.customer.CustomerBookingService;
import com.github.b3kt.infrastructure.security.Roles;
import com.github.b3kt.presentation.exception.RestErrorLogger;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** Staff review queue for customer service bookings: confirm, reject, or convert to a work order. */
@Path("/api/pazaauto/customer-bookings")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({Roles.ADMIN, Roles.OWNER})
@RequestScoped
public class CustomerBookingAdminResource {

    @Inject
    CustomerBookingService bookingService;

    @GET
    public Response list(@QueryParam("status") String status) {
        return Response.ok(ApiResponse.success(bookingService.listForAdmin(status))).build();
    }

    @POST
    @Path("/{id}/confirm")
    public Response confirm(@PathParam("id") Long id, ReviewRequest request) {
        try {
            bookingService.confirm(id, request != null ? request.getNote() : null);
            return Response.ok(ApiResponse.success("Booking confirmed", null)).build();
        } catch (IllegalArgumentException e) {
            RestErrorLogger.log(Response.Status.BAD_REQUEST, e);
            return Response.status(Response.Status.BAD_REQUEST).entity(ApiResponse.error(e.getMessage())).build();
        }
    }

    @POST
    @Path("/{id}/reject")
    public Response reject(@PathParam("id") Long id, ReviewRequest request) {
        try {
            bookingService.reject(id, request != null ? request.getNote() : null);
            return Response.ok(ApiResponse.success("Booking rejected", null)).build();
        } catch (IllegalArgumentException e) {
            RestErrorLogger.log(Response.Status.BAD_REQUEST, e);
            return Response.status(Response.Status.BAD_REQUEST).entity(ApiResponse.error(e.getMessage())).build();
        }
    }

    @POST
    @Path("/{id}/convert-to-spk")
    public Response convertToSpk(@PathParam("id") Long id, ConvertToSpkRequest request) {
        try {
            Long spkId = bookingService.convertToSpk(id, request);
            return Response.ok(ApiResponse.success("Converted to work order", java.util.Map.of("spkId", spkId))).build();
        } catch (IllegalArgumentException e) {
            RestErrorLogger.log(Response.Status.BAD_REQUEST, e);
            return Response.status(Response.Status.BAD_REQUEST).entity(ApiResponse.error(e.getMessage())).build();
        }
    }
}
