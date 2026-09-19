package com.github.b3kt.presentation.rest;

import com.github.b3kt.application.dto.LoginResponse;
import com.github.b3kt.application.service.AuthService;
import com.github.b3kt.application.service.UserService;
import com.github.b3kt.infrastructure.persistence.entity.UserEntity;
import com.github.b3kt.infrastructure.security.PasswordChangeRequiredAugmentor;
import com.github.b3kt.infrastructure.security.Roles;
import com.github.b3kt.integration.IntegrationTestBase;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.smallrye.jwt.build.Jwt;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * A token issued for a temporary password only allows changing the password (and logout / current user).
 */
@QuarkusTest
@DisplayName("Password change requirement")
class PasswordChangeRequiredTest extends IntegrationTestBase {

    @InjectMock
    AuthService authService;

    @InjectMock
    UserService userService;

    @ConfigProperty(name = "mp.jwt.verify.issuer")
    String issuer;

    private String token(boolean mustChangePassword) {
        var builder = Jwt.issuer(issuer).upn("owner").subject("owner").groups(Set.of(Roles.OWNER));
        if (mustChangePassword) {
            builder.claim(PasswordChangeRequiredAugmentor.CLAIM, true);
        }
        return builder.sign();
    }

    @Test
    @DisplayName("Role-protected endpoints are refused until the password is changed")
    void blocksRoleEndpoints() {
        when(userService.findAll()).thenReturn(List.of());

        given().auth().oauth2(token(true)).when().get("/api/users").then().statusCode(403);
        given().auth().oauth2(token(false)).when().get("/api/users").then().statusCode(200);
    }

    @Test
    @DisplayName("Change password, logout and current user stay available")
    void allowsAccountEndpoints() {
        LoginResponse response = new LoginResponse("t", "r", "owner", "o@example.com", 1800L);
        when(authService.changePassword("owner", "Temp-password1", "New-password-1")).thenReturn(response);

        given().auth().oauth2(token(true)).contentType(ContentType.JSON)
                .body("{\"currentPassword\": \"Temp-password1\", \"newPassword\": \"New-password-1\"}")
                .when().post("/api/auth/change-password")
                .then().statusCode(200).body("data.token", equalTo("t"));

        given().auth().oauth2(token(true)).contentType(ContentType.JSON)
                .when().post("/api/auth/logout").then().statusCode(200);
    }

    @Test
    @DisplayName("Policy violations and a wrong current password are reported")
    void changePasswordErrors() {
        when(authService.changePassword(eq("owner"), eq("wrong"), anyString()))
                .thenThrow(new com.github.b3kt.domain.exception.AuthenticationException("Current password is incorrect"));
        when(authService.changePassword(eq("owner"), eq("Temp-password1"), eq("short")))
                .thenThrow(new IllegalArgumentException("Password must be at least 8 characters"));

        given().auth().oauth2(token(true)).contentType(ContentType.JSON)
                .body("{\"currentPassword\": \"wrong\", \"newPassword\": \"New-password-1\"}")
                .when().post("/api/auth/change-password").then().statusCode(401);
        given().auth().oauth2(token(true)).contentType(ContentType.JSON)
                .body("{\"currentPassword\": \"Temp-password1\", \"newPassword\": \"short\"}")
                .when().post("/api/auth/change-password").then().statusCode(400);
        given().contentType(ContentType.JSON)
                .body("{\"currentPassword\": \"a\", \"newPassword\": \"b\"}")
                .when().post("/api/auth/change-password").then().statusCode(401);
    }

    @Test
    @DisplayName("User API never returns the password hash")
    void userApiHidesHash() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setUsername("budi");
        user.setPasswordHash("$2a$10$secret-hash");
        when(userService.findById(1L)).thenReturn(user);

        given().auth().oauth2(token(false)).when().get("/api/users/1")
                .then().statusCode(200)
                .body(not(containsString("secret-hash")))
                .body("data.passwordHash", nullValue())
                .body("data.password", nullValue());
    }
}
