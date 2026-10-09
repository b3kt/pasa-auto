package com.github.b3kt.presentation.rest;

import com.github.b3kt.application.dto.ApiResponse;
import com.github.b3kt.application.service.AppSettingService;
import com.github.b3kt.infrastructure.security.CustomerAccessGateFilter;
import com.github.b3kt.infrastructure.security.Roles;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** Admin toggle for the customer self-service portal's master switch. */
@Path("/api/settings/customer-access")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class AppSettingResource {

    @Inject
    AppSettingService appSettingService;

    @Inject
    SecurityIdentity identity;

    public record ToggleRequest(boolean enabled) {
    }

    @GET
    @RolesAllowed({Roles.ADMIN, Roles.OWNER})
    public Response get() {
        boolean enabled = appSettingService.getBooleanConfig(CustomerAccessGateFilter.CUSTOMER_ACCESS_ENABLED_KEY, false);
        return Response.ok(ApiResponse.success(java.util.Map.of("enabled", enabled))).build();
    }

    @PUT
    @RolesAllowed(Roles.OWNER)
    public Response set(ToggleRequest request) {
        appSettingService.setBooleanConfig(CustomerAccessGateFilter.CUSTOMER_ACCESS_ENABLED_KEY, request.enabled(),
                "Master switch for the customer self-service portal", identity.getPrincipal().getName());
        return Response.ok(ApiResponse.success("Setting updated", java.util.Map.of("enabled", request.enabled()))).build();
    }
}
