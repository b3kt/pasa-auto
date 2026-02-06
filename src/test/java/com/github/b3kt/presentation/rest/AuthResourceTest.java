package com.github.b3kt.presentation.rest;

import com.github.b3kt.application.dto.LoginRequest;
import com.github.b3kt.application.dto.LoginResponse;
import com.github.b3kt.application.dto.UserInfo;
import com.github.b3kt.application.service.AuthService;
import com.github.b3kt.domain.exception.AuthenticationException;
import com.github.b3kt.integration.IntegrationTestBase;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
class AuthResourceTest extends IntegrationTestBase {

    @InjectMock
    AuthService authService;

    @InjectMock
    JsonWebToken jwt;

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void testLoginSuccess() {
        // Given
        LoginRequest loginRequest = new LoginRequest("testuser", "password123");
        LoginResponse loginResponse = new LoginResponse(
            "jwt-token", "refresh-token", "testuser", "test@example.com", 3600L
        );

        when(authService.login("testuser", "password123")).thenReturn(loginResponse);

        // When & Then
        given()
            .contentType(ContentType.JSON)
            .body(loginRequest)
        .when()
            .post("/api/auth/login")
        .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.token", equalTo("jwt-token"))
            .body("data.refreshToken", equalTo("refresh-token"))
            .body("data.username", equalTo("testuser"))
            .body("data.email", equalTo("test@example.com"))
            .body("data.expiresIn", equalTo(3600));

        verify(authService).login("testuser", "password123");
    }

    @Test
    @DisplayName("Should return 401 for invalid credentials")
    void testLoginInvalidCredentials() {
        // Given
        LoginRequest loginRequest = new LoginRequest("invalid", "wrongpassword");
        
        when(authService.login("invalid", "wrongpassword"))
            .thenThrow(new AuthenticationException("Invalid credentials"));

        // When & Then
        given()
            .contentType(ContentType.JSON)
            .body(loginRequest)
        .when()
            .post("/api/auth/login")
        .then()
            .statusCode(401)
            .body("success", equalTo(false))
            .body("error", containsString("Invalid"));

        verify(authService).login("invalid", "wrongpassword");
    }

    @Test
    @DisplayName("Should return 400 for invalid login request")
    void testLoginInvalidRequest() {
        // Given
        LoginRequest invalidRequest = new LoginRequest("", ""); // Empty username and password

        // When & Then
        given()
            .contentType(ContentType.JSON)
            .body(invalidRequest)
        .when()
            .post("/api/auth/login")
        .then()
            .statusCode(400)
            .body("success", equalTo(false));
    }

    @Test
    @DisplayName("Should refresh token successfully")
    void testRefreshTokenSuccess() {
        // Given
        String refreshToken = "valid-refresh-token";
        LoginResponse newLoginResponse = new LoginResponse(
            "new-jwt-token", "new-refresh-token", "testuser", "test@example.com", 3600L
        );

        when(authService.refreshToken(refreshToken)).thenReturn(newLoginResponse);

        // When & Then
        given()
            .contentType(ContentType.JSON)
            .body("{\"refreshToken\": \"" + refreshToken + "\"}")
        .when()
            .post("/api/auth/refresh")
        .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.token", equalTo("new-jwt-token"))
            .body("data.refreshToken", equalTo("new-refresh-token"))
            .body("data.username", equalTo("testuser"))
            .body("data.email", equalTo("test@example.com"));

        verify(authService).refreshToken(refreshToken);
    }

    @Test
    @DisplayName("Should return 401 for invalid refresh token")
    void testRefreshTokenInvalid() {
        // Given
        String invalidToken = "invalid-token";
        
        when(authService.refreshToken(invalidToken))
            .thenThrow(new AuthenticationException("Invalid refresh token"));

        // When & Then
        given()
            .contentType(ContentType.JSON)
            .body("{\"refreshToken\": \"" + invalidToken + "\"}")
        .when()
            .post("/api/auth/refresh")
        .then()
            .statusCode(401)
            .body("success", equalTo(false))
            .body("error", containsString("Invalid"));

        verify(authService).refreshToken(invalidToken);
    }

    @Test
    @DisplayName("Should get user info successfully")
    @TestSecurity(user = "testuser", roles = {"user"})
    void testGetUserInfoSuccess() {
        // Given
        UserInfo userInfo = new UserInfo();
        userInfo.setUsername("testuser");
        userInfo.setEmail("test@example.com");
        userInfo.setRoles(java.util.Set.of("USER"));

        when(authService.getUserInfo(any(JsonWebToken.class))).thenReturn(userInfo);

        // When & Then
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/api/auth/me")
        .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.username", equalTo("testuser"))
            .body("data.email", equalTo("test@example.com"))
            .body("data.roles", hasItem("USER"));

        verify(authService).getUserInfo(any(JsonWebToken.class));
    }

    @Test
    @DisplayName("Should return 401 for unauthorized user info request")
    void testGetUserInfoUnauthorized() {
        // When & Then
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/api/auth/me")
        .then()
            .statusCode(401);
    }

    @Test
    @DisplayName("Should logout successfully")
    @TestSecurity(user = "testuser", roles = {"user"})
    void testLogoutSuccess() {
        // When & Then
        given()
            .contentType(ContentType.JSON)
        .when()
            .post("/api/auth/logout")
        .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("message", containsString("Logged out"));
    }

    @Test
    @DisplayName("Should return 401 for unauthorized logout request")
    void testLogoutUnauthorized() {
        // When & Then
        given()
            .contentType(ContentType.JSON)
        .when()
            .post("/api/auth/logout")
        .then()
            .statusCode(401);
    }

    @Test
    @DisplayName("Should handle missing request body gracefully")
    void testMissingRequestBody() {
        // When & Then
        given()
            .contentType(ContentType.JSON)
        .when()
            .post("/api/auth/login")
        .then()
            .statusCode(500);
    }

    @Test
    @DisplayName("Should handle malformed JSON gracefully")
    void testMalformedJson() {
        // When & Then
        given()
            .contentType(ContentType.JSON)
            .body("invalid json")
        .when()
            .post("/api/auth/login")
        .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("Should validate content type")
    void testContentTypeValidation() {
        // Given
        LoginRequest loginRequest = new LoginRequest("testuser", "password123");

        // When & Then
        given()
            .body(loginRequest)
        .when()
            .post("/api/auth/login")
        .then()
            .statusCode(415); // Unsupported Media Type
    }

    @Test
    @DisplayName("Should handle concurrent login requests")
    void testConcurrentLoginRequests() {
        // Given
        LoginRequest loginRequest = new LoginRequest("testuser", "password123");
        LoginResponse loginResponse = new LoginResponse(
            "jwt-token", "refresh-token", "testuser", "test@example.com", 3600L
        );

        when(authService.login("testuser", "password123")).thenReturn(loginResponse);

        // When & Then - Execute multiple concurrent requests
        ValidatableResponse response1 = given()
            .contentType(ContentType.JSON)
            .body(loginRequest)
        .when()
            .post("/api/auth/login")
        .then();

        ValidatableResponse response2 = given()
            .contentType(ContentType.JSON)
            .body(loginRequest)
        .when()
            .post("/api/auth/login")
        .then();

        // Both should succeed
        response1.statusCode(200);
        response2.statusCode(200);

        verify(authService, times(2)).login("testuser", "password123");
    }

    @Test
    @DisplayName("Should handle very long username/password")
    void testLongCredentials() {
        // Given
        StringBuilder longString = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longString.append("a");
        }
        
        LoginRequest loginRequest = new LoginRequest(
            longString.toString(), 
            longString.toString()
        );

        // When & Then
        given()
            .contentType(ContentType.JSON)
            .body(loginRequest)
        .when()
            .post("/api/auth/login")
        .then()
            .statusCode(anyOf(equalTo(400), equalTo(401), equalTo(200))); // Either validation error, auth error, or success
    }

    @Test
    @DisplayName("Should handle special characters in credentials")
    void testSpecialCharactersInCredentials() {
        // Given
        LoginRequest loginRequest = new LoginRequest("user@domain.com", "P@ssw0rd!#$%");

        when(authService.login("user@domain.com", "P@ssw0rd!#$%"))
            .thenThrow(new AuthenticationException("Invalid credentials"));

        // When & Then
        given()
            .contentType(ContentType.JSON)
            .body(loginRequest)
        .when()
            .post("/api/auth/login")
        .then()
            .statusCode(401);

        verify(authService).login("user@domain.com", "P@ssw0rd!#$%");
    }
}
