package com.github.b3kt.presentation.rest;

import com.github.b3kt.infrastructure.security.Roles;
import com.github.b3kt.application.dto.ApiResponse;
import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheManager;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Admin controls for the server-side (Caffeine-backed) Quarkus caches, as distinct from the
 * browser-side master data cache managed by {@code masterDataCache.js}.
 */
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
@Path("/api/admin/caffeine-cache")
@RolesAllowed({Roles.ADMIN, Roles.OWNER})
public class CacheAdminResource {

    @Inject
    CacheManager cacheManager;

    @POST
    @Path("/clear")
    public Response clearAll() {
        int count = 0;
        for (String cacheName : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(cacheName).orElse(null);
            if (cache != null) {
                cache.invalidateAll().await().indefinitely();
                count++;
            }
        }
        return Response.ok(ApiResponse.success("Caffeine cache berhasil dihapus", count)).build();
    }
}
