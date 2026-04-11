package com.github.b3kt.presentation.rest;

import com.github.b3kt.application.dto.ApiResponse;
import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple health check endpoint for Electron app monitoring
 */
@Path("/health")
@Produces(MediaType.APPLICATION_JSON)
@PermitAll
@Tag(name = "Health", description = "Application health monitoring")
@RequestScoped
public class HealthResource {

    @GET
    @Operation(
        summary = "Health check",
        description = "Simple health check endpoint for monitoring"
    )
    @APIResponse(
        responseCode = "200",
        description = "Application is healthy",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = ApiResponse.class)
        )
    )
    public Response health() {
        Map<String, Object> healthData = new HashMap<>();
        healthData.put("status", "UP");
        healthData.put("timestamp", LocalDateTime.now());
        healthData.put("application", "pasa-auto");
        healthData.put("version", "0.0.14");
        
        return Response.ok(ApiResponse.success("Application is healthy", healthData)).build();
    }
}
