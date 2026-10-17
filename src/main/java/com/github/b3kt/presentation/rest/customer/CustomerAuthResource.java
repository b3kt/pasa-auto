package com.github.b3kt.presentation.rest.customer;

import com.github.b3kt.application.dto.ApiResponse;
import com.github.b3kt.application.dto.RefreshTokenRequest;
import com.github.b3kt.application.dto.customer.CustomerLoginRequest;
import com.github.b3kt.application.dto.customer.CustomerLoginResponse;
import com.github.b3kt.application.dto.customer.CustomerRegisterRequest;
import com.github.b3kt.application.service.LoginAttemptService;
import com.github.b3kt.application.service.customer.CustomerAuthService;
import com.github.b3kt.domain.exception.AuthenticationException;
import com.github.b3kt.infrastructure.security.CustomerPrincipal;
import com.github.b3kt.infrastructure.security.Roles;
import com.github.b3kt.presentation.rest.AuthCookies;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.SocketAddress;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

/** Registration/login/session endpoints for the customer portal, independent of {@code /api/auth}. */
@Path("/api/customer/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class CustomerAuthResource {

    private static final String REFRESH_COOKIE = AuthCookies.CUSTOMER_REFRESH_COOKIE;

    @Inject
    CustomerAuthService authService;

    @Inject
    LoginAttemptService loginAttemptService;

    @Inject
    CustomerPrincipal principal;

    @Inject
    com.github.b3kt.infrastructure.security.CustomerJwtTokenService jwtTokenService;

    @Inject
    AuthCookies authCookies;

    @Context
    HttpServerRequest request;

    @POST
    @Path("/register")
    @PermitAll
    public Response register(@Valid CustomerRegisterRequest registerRequest) {
        try {
            CustomerLoginResponse response = authService.register(registerRequest);
            return withRefreshCookie(Response.ok(ApiResponse.success("Registration successful", response)), response);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.<CustomerLoginResponse>error(e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/login")
    @PermitAll
    public Response login(@Valid CustomerLoginRequest loginRequest) {
        String clientIp = clientIp();
        // One counter per account target whatever the spelling ("+62 812..." and "0812...", any email
        // case), in its own namespace so it never shares a lockout with a staff username.
        String attemptKey = "customer-login:" + CustomerAuthService.loginKey(loginRequest.getIdentifier());
        loginAttemptService.checkAllowed(attemptKey, clientIp);
        try {
            CustomerLoginResponse response = authService.login(loginRequest.getIdentifier(), loginRequest.getPassword());
            loginAttemptService.recordSuccess(attemptKey);
            return withRefreshCookie(Response.ok(ApiResponse.success("Login successful", response)), response);
        } catch (AuthenticationException e) {
            loginAttemptService.recordFailure(attemptKey, clientIp);
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(ApiResponse.<CustomerLoginResponse>error(e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/refresh")
    @PermitAll
    public Response refreshToken(RefreshTokenRequest body, @CookieParam(REFRESH_COOKIE) String refreshCookie) {
        String refreshToken = refreshCookie != null && !refreshCookie.isBlank()
                ? refreshCookie
                : (body != null ? body.getRefreshToken() : null);
        if (refreshToken == null || refreshToken.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(ApiResponse.<CustomerLoginResponse>error("Invalid or expired refresh token"))
                    .build();
        }
        try {
            CustomerLoginResponse response = authService.refreshToken(refreshToken);
            return withRefreshCookie(Response.ok(ApiResponse.success("Token refreshed successfully", response)), response);
        } catch (AuthenticationException e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(ApiResponse.<CustomerLoginResponse>error(e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/logout")
    @RolesAllowed(Roles.CUSTOMER)
    public Response logout(RefreshTokenRequest body, @CookieParam(REFRESH_COOKIE) String refreshCookie) {
        String refreshToken = refreshCookie != null && !refreshCookie.isBlank()
                ? refreshCookie
                : (body != null ? body.getRefreshToken() : null);
        authService.logout(principal.customerAccountId(), refreshToken);
        return Response.ok(ApiResponse.success("Logged out successfully", null))
                .cookie(authCookies.customerRefresh(request, null, 0))
                .build();
    }

    @GET
    @Path("/me")
    @RolesAllowed(Roles.CUSTOMER)
    public Response me() {
        return Response.ok(ApiResponse.success(java.util.Map.of(
                "accountId", principal.customerAccountId(),
                "linked", principal.pelangganIdOrNull() != null))).build();
    }

    @POST
    @Path("/change-password")
    @RolesAllowed(Roles.CUSTOMER)
    public Response changePassword(@Valid com.github.b3kt.application.dto.ChangePasswordRequest changeRequest) {
        String clientIp = clientIp();
        Long accountId = principal.customerAccountId();
        loginAttemptService.checkAllowed("customer:" + accountId, clientIp);
        try {
            CustomerLoginResponse response = authService.changePassword(
                    accountId, changeRequest.getCurrentPassword(), changeRequest.getNewPassword());
            loginAttemptService.recordSuccess("customer:" + accountId);
            return withRefreshCookie(Response.ok(ApiResponse.success("Password changed successfully", response)), response);
        } catch (AuthenticationException e) {
            loginAttemptService.recordFailure("customer:" + accountId, clientIp);
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(ApiResponse.<CustomerLoginResponse>error(e.getMessage()))
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.<CustomerLoginResponse>error(e.getMessage()))
                    .build();
        }
    }

    private String clientIp() {
        SocketAddress remoteAddress = request != null ? request.remoteAddress() : null;
        return remoteAddress != null ? remoteAddress.hostAddress() : null;
    }

    private Response withRefreshCookie(Response.ResponseBuilder builder, CustomerLoginResponse response) {
        if (response == null) {
            return builder.build();
        }
        String refreshToken = response.getRefreshToken();
        response.setRefreshToken(null);
        if (refreshToken == null) {
            return builder.build();
        }
        return builder.cookie(refreshCookie(refreshToken, jwtTokenService.getRefreshTokenLifetime().toSeconds())).build();
    }

    private NewCookie refreshCookie(String value, long maxAgeSeconds) {
        return authCookies.customerRefresh(request, value, maxAgeSeconds);
    }
}
