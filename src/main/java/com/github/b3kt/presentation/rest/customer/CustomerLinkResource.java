package com.github.b3kt.presentation.rest.customer;

import com.github.b3kt.application.dto.ApiResponse;
import com.github.b3kt.application.dto.customer.LinkAccountRequest;
import com.github.b3kt.application.dto.customer.LinkRequestResultDto;
import com.github.b3kt.application.dto.customer.LinkSearchResultDto;
import com.github.b3kt.application.service.customer.CustomerLinkService;
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

import java.util.List;

/** Lets a customer find and claim their existing pelanggan/vehicle history. */
@Path("/api/customer/link")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed(Roles.CUSTOMER)
@RequestScoped
public class CustomerLinkResource {

    @Inject
    CustomerLinkService linkService;

    @Inject
    CustomerPrincipal principal;

    @GET
    @Path("/search-by-nopol/{nopol}")
    public Response searchByNopol(@PathParam("nopol") String nopol) {
        LinkSearchResultDto result = linkService.searchByNopol(nopol);
        if (result == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ApiResponse.error("No unlinked vehicle record found with that plate number"))
                    .build();
        }
        return Response.ok(ApiResponse.success(result)).build();
    }

    @GET
    @Path("/search-by-contact")
    public Response searchByContact(@QueryParam("phone") String phone, @QueryParam("email") String email) {
        List<LinkSearchResultDto> results = linkService.searchByContact(phone, email);
        return Response.ok(ApiResponse.success(results)).build();
    }

    @POST
    @Path("/skip")
    public Response skip() {
        try {
            linkService.skipLinking(principal.customerAccountId());
            return Response.ok(ApiResponse.success("Account left unlinked; placeholder customer record created if needed", null)).build();
        } catch (IllegalArgumentException e) {
            RestErrorLogger.log(Response.Status.BAD_REQUEST, e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error(e.getMessage()))
                    .build();
        }
    }
}
