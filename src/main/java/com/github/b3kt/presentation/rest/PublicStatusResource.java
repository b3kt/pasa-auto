package com.github.b3kt.presentation.rest;

import com.github.b3kt.application.dto.ApiResponse;
import com.github.b3kt.application.service.AppSettingService;
import com.github.b3kt.infrastructure.security.CustomerAccessGateFilter;
import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** Unauthenticated status checks the SPA needs before anyone has signed in. */
@Path("/api/public")
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class PublicStatusResource {

    @Inject
    AppSettingService appSettingService;

    @GET
    @Path("/customer-access-status")
    @PermitAll
    public Response customerAccessStatus() {
        boolean enabled = appSettingService.getBooleanConfig(CustomerAccessGateFilter.CUSTOMER_ACCESS_ENABLED_KEY, false);
        return Response.ok(ApiResponse.success(java.util.Map.of("customerAccessEnabled", enabled))).build();
    }
}
