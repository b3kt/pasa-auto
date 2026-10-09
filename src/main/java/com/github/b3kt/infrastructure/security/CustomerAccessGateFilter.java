package com.github.b3kt.infrastructure.security;

import com.github.b3kt.application.dto.ApiResponse;
import com.github.b3kt.application.service.AppSettingService;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

/**
 * Master switch for the customer self-service portal. Runs {@code @PreMatching}, before
 * {@code @RolesAllowed} is ever evaluated, so a disabled portal returns a clean 503 rather than a
 * 401 that would otherwise leak "this endpoint exists but you're unauthorized" to an outsider.
 */
@Provider
@PreMatching
@Priority(Priorities.AUTHENTICATION - 100)
public class CustomerAccessGateFilter implements ContainerRequestFilter {

    public static final String CUSTOMER_ACCESS_ENABLED_KEY = "customer.access.enabled";

    @Inject
    AppSettingService appSettingService;

    @Override
    public void filter(ContainerRequestContext ctx) {
        // getPath() has no leading slash per the JAX-RS spec, but is defensively normalized here
        // since that has been observed to vary by implementation/container.
        String path = ctx.getUriInfo().getPath();
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path.startsWith("api/customer/") && !appSettingService.getBooleanConfig(CUSTOMER_ACCESS_ENABLED_KEY, false)) {
            ctx.abortWith(Response.status(503)
                    .entity(ApiResponse.error("Customer portal is temporarily unavailable"))
                    .build());
        }
    }
}
