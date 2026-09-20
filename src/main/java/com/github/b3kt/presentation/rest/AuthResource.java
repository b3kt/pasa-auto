package com.github.b3kt.presentation.rest;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.SocketAddress;
import com.github.b3kt.application.service.LoginAttemptService;
import com.github.b3kt.application.dto.ApiResponse;
import com.github.b3kt.application.dto.LoginRequest;
import com.github.b3kt.application.dto.LoginResponse;
import com.github.b3kt.application.dto.UserInfo;
import com.github.b3kt.application.service.AuthService;
import com.github.b3kt.domain.exception.AuthenticationException;
import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
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

    @Inject
    SecurityIdentity identity;

    @Inject
    LoginAttemptService loginAttemptService;

    @Context
    HttpServerRequest request;

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
        String clientIp = clientIp();
        loginAttemptService.checkAllowed(loginRequest.getUsername(), clientIp);
        try {
            LoginResponse response = authService.login(
                loginRequest.getUsername(),
                loginRequest.getPassword()
            );
            loginAttemptService.recordSuccess(loginRequest.getUsername());
            return Response.ok(ApiResponse.success("Login successful", response)).build();
        } catch (AuthenticationException e) {
            loginAttemptService.recordFailure(loginRequest.getUsername(), clientIp);
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(ApiResponse.<LoginResponse>error(e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/logout")
    @Authenticated
    @Operation(
        summary = "User logout",
        description = "Revoke the session of the given refresh token. Without a refresh token, all sessions of the current user are revoked."
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
    public Response logout(com.github.b3kt.application.dto.RefreshTokenRequest request) {
        // The access token stays valid until it expires (short-lived); revoking the refresh token ends the session
        authService.logout(identity.getPrincipal().getName(), request != null ? request.getRefreshToken() : null);
        return Response.ok(ApiResponse.success("Logged out successfully", null)).build();
    }

    @GET
    @Path("/me")
    @Authenticated
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
    @Path("/change-password")
    @Authenticated
    @Operation(
        summary = "Change password",
        description = "Change the current user's password. Also required after logging in with a temporary password. "
                + "Returns new tokens; the user's other sessions are ended."
    )
    @SecurityRequirement(name = "bearerAuth")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Password changed",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiResponse.class))),
        @APIResponse(responseCode = "400", description = "New password does not meet the password policy",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiResponse.class))),
        @APIResponse(responseCode = "401", description = "Current password is incorrect or not authenticated",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiResponse.class)))
    })
    public Response changePassword(@Valid com.github.b3kt.application.dto.ChangePasswordRequest request) {
        // A stolen session must not be usable to guess the current password without limit
        String username = identity.getPrincipal().getName();
        String clientIp = clientIp();
        loginAttemptService.checkAllowed(username, clientIp);
        try {
            LoginResponse response = authService.changePassword(
                username, request.getCurrentPassword(), request.getNewPassword());
            loginAttemptService.recordSuccess(username);
            return Response.ok(ApiResponse.success("Password changed successfully", response)).build();
        } catch (AuthenticationException e) {
            loginAttemptService.recordFailure(username, clientIp);
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(ApiResponse.<LoginResponse>error(e.getMessage()))
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.<LoginResponse>error(e.getMessage()))
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
    public Response refreshToken(@Valid com.github.b3kt.application.dto.RefreshTokenRequest request) {
        try {
            LoginResponse response = authService.refreshToken(request.getRefreshToken());
            return Response.ok(ApiResponse.success("Token refreshed successfully", response)).build();
        } catch (AuthenticationException e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(ApiResponse.<LoginResponse>error(e.getMessage()))
                    .build();
        }
    }

    /** The connection address; see TbAbsensiResource for why forwarding headers aren't read here. */
    private String clientIp() {
        SocketAddress remoteAddress = request != null ? request.remoteAddress() : null;
        return remoteAddress != null ? remoteAddress.hostAddress() : null;
    }
}
