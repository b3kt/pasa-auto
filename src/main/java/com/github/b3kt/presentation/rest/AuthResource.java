package com.github.b3kt.presentation.rest;

import com.github.b3kt.application.dto.ApiResponse;
import com.github.b3kt.application.dto.LoginRequest;
import com.github.b3kt.application.dto.LoginResponse;
import com.github.b3kt.application.dto.UserInfo;
import com.github.b3kt.application.service.AuthService;
import com.github.b3kt.domain.exception.AuthenticationException;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * REST controller for authentication endpoints.
 * This is the presentation layer that handles HTTP requests.
 */
@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Authentication", description = "Authentication and user management endpoints")
@RequestScoped
public class AuthResource {

    @Inject
    AuthService authService;

    @Inject
    JsonWebToken jwt;

    @POST
    @Path("/login")
    @PermitAll
    @Operation(
        summary = "User login",
        description = "Authenticate user with username and password, returns JWT token"
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Login successful",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = ApiResponse.class)
            )
        ),
        @APIResponse(
            responseCode = "401",
            description = "Invalid credentials",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = ApiResponse.class)
            )
        ),
        @APIResponse(
            responseCode = "400",
            description = "Validation error",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = ApiResponse.class)
            )
        )
    })
    public Response login(@Valid LoginRequest loginRequest) {
        try {
            LoginResponse response = authService.login(
                loginRequest.getUsername(),
                loginRequest.getPassword()
            );
            return Response.ok(ApiResponse.success("Login successful", response)).build();
        } catch (AuthenticationException e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(ApiResponse.<LoginResponse>error(e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/logout")
    @RolesAllowed("user")
    @Operation(
        summary = "User logout",
        description = "Logout the current user. In JWT systems, this is typically handled client-side."
    )
    @SecurityRequirement(name = "bearerAuth")
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Logout successful",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = ApiResponse.class)
            )
        ),
        @APIResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing token"
        )
    })
    public Response logout() {
        // In a stateless JWT system, logout is typically handled client-side
        // by removing the token. For server-side logout, you'd need a token blacklist.
        return Response.ok(ApiResponse.success("Logged out successfully", null)).build();
    }

    @GET
    @Path("/me")
    @RolesAllowed("user")
    @Operation(
        summary = "Get current user info",
        description = "Retrieve information about the currently authenticated user from JWT token"
    )
    @SecurityRequirement(name = "bearerAuth")
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "User information retrieved successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = ApiResponse.class)
            )
        ),
        @APIResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or expired token",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = ApiResponse.class)
            )
        )
    })
    public Response getCurrentUser() {
        try {
            UserInfo userInfo = authService.getUserInfo(jwt);
            return Response.ok(ApiResponse.success(userInfo)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(ApiResponse.<UserInfo>error("Invalid or expired token"))
                    .build();
        }
    }
    
    @POST
    @Path("/refresh")
    @PermitAll
    @Operation(
        summary = "Refresh access token",
        description = "Use a refresh token to obtain new access and refresh tokens"
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Token refreshed successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = ApiResponse.class)
            )
        ),
        @APIResponse(
            responseCode = "401",
            description = "Invalid or expired refresh token",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = ApiResponse.class)
            )
        )
    })
    public Response refreshToken(com.github.b3kt.application.dto.RefreshTokenRequest request) {
        try {
            LoginResponse response = authService.refreshToken(request.getRefreshToken());
            return Response.ok(ApiResponse.success("Token refreshed successfully", response)).build();
        } catch (AuthenticationException e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(ApiResponse.<LoginResponse>error(e.getMessage()))
                    .build();
        }
    }
}

