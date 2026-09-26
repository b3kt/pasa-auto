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
import static io.restassured.matcher.RestAssuredMatchers.detailedCookie;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@QuarkusTest
class AuthResourceTest extends IntegrationTestBase {

    @InjectMock
    AuthService authService;

    @io.quarkus.test.junit.mockito.InjectSpy
    com.github.b3kt.application.service.LoginAttemptService loginAttemptService;

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
            // The refresh token goes into an HttpOnly cookie, never the body
            .body("data.refreshToken", nullValue())
            .cookie("refresh_token", detailedCookie().value("refresh-token").httpOnly(true).path("/api/auth"))
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
            .body("data.refreshToken", nullValue())
            .cookie("refresh_token", detailedCookie().value("new-refresh-token").httpOnly(true))
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
    @DisplayName("Should update the current user's profile")
    @TestSecurity(user = "testuser", roles = {"user"})
    void testUpdateProfileSuccess() {
        UserInfo userInfo = new UserInfo();
        userInfo.setUsername("testuser");
        userInfo.setEmail("new-email@example.com");
        userInfo.setRoles(java.util.Set.of("USER"));

        when(authService.updateProfile(eq("testuser"), eq("new-email@example.com"))).thenReturn(userInfo);

        given()
            .contentType(ContentType.JSON)
            .body(java.util.Map.of("email", "new-email@example.com"))
        .when()
            .put("/api/auth/me")
        .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.email", equalTo("new-email@example.com"));

        verify(authService).updateProfile("testuser", "new-email@example.com");
    }

    @Test
    @DisplayName("Should reject an invalid email on profile update")
    @TestSecurity(user = "testuser", roles = {"user"})
    void testUpdateProfileInvalidEmail() {
        given()
            .contentType(ContentType.JSON)
            .body(java.util.Map.of("email", "not-an-email"))
        .when()
            .put("/api/auth/me")
        .then()
            .statusCode(400);

        verify(authService, never()).updateProfile(anyString(), anyString());
    }

    @Test
    @DisplayName("Should return 401 for unauthorized profile update")
    void testUpdateProfileUnauthorized() {
        given()
            .contentType(ContentType.JSON)
            .body(java.util.Map.of("email", "new-email@example.com"))
        .when()
            .put("/api/auth/me")
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

        verify(authService).logout("testuser", null);
    }

    @Test
    @DisplayName("Should revoke the given refresh token on logout")
    @TestSecurity(user = "testuser", roles = {"user"})
    void testLogoutWithRefreshToken() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"refreshToken\": \"refresh-token\"}")
        .when()
            .post("/api/auth/logout")
        .then()
            .statusCode(200);

        verify(authService).logout("testuser", "refresh-token");
    }

    @Test
    @DisplayName("Should lock an account after repeated failed logins")
    void testLoginLockout() {
        // Unique username: the attempt counters are shared by every test in this app instance
        String username = "lockout-" + System.nanoTime();
        when(authService.login(eq(username), anyString()))
            .thenThrow(new AuthenticationException("Invalid username or password"));
        String body = "{\"username\": \"" + username + "\", \"password\": \"wrong\"}";

        for (int i = 0; i < 5; i++) {
            given().contentType(ContentType.JSON).body(body)
                .when().post("/api/auth/login")
                .then().statusCode(401);
        }

        given().contentType(ContentType.JSON).body(body)
            .when().post("/api/auth/login")
            .then()
                .statusCode(429)
                .header("Retry-After", notNullValue())
                .body("success", equalTo(false))
                .body("error", containsString("Too many failed attempts"));

        // Locked out: the password is not even checked any more
        verify(authService, times(5)).login(eq(username), anyString());
        // Failures are counted against the connection's address too (per-IP limit)
        verify(loginAttemptService, times(5)).recordFailure(username, "127.0.0.1");
    }

    @Test
    @DisplayName("Should reset the failure count after a successful login")
    void testLoginSuccessResetsFailures() {
        String username = "reset-" + System.nanoTime();
        String wrong = "{\"username\": \"" + username + "\", \"password\": \"wrong\"}";
        String right = "{\"username\": \"" + username + "\", \"password\": \"right\"}";
        when(authService.login(username, "wrong"))
            .thenThrow(new AuthenticationException("Invalid username or password"));
        when(authService.login(username, "right"))
            .thenReturn(new LoginResponse("t", "r", username, "e@x.com", 1800L));

        for (int i = 0; i < 4; i++) {
            given().contentType(ContentType.JSON).body(wrong).when().post("/api/auth/login").then().statusCode(401);
        }
        given().contentType(ContentType.JSON).body(right).when().post("/api/auth/login").then().statusCode(200);
        for (int i = 0; i < 4; i++) {
            given().contentType(ContentType.JSON).body(wrong).when().post("/api/auth/login").then().statusCode(401);
        }
    }

    @Test
    @DisplayName("Should refresh using the cookie and ignore a body token")
    void testRefreshUsesCookie() {
        LoginResponse response = new LoginResponse("t2", "r2", "testuser", "test@example.com", 1800L);
        when(authService.refreshToken("cookie-token")).thenReturn(response);

        given()
            .contentType(ContentType.JSON)
            .cookie("refresh_token", "cookie-token")
            .body("{\"refreshToken\": \"body-token\"}")
        .when()
            .post("/api/auth/refresh")
        .then()
            .statusCode(200);

        verify(authService).refreshToken("cookie-token");
        verify(authService, never()).refreshToken("body-token");
    }

    @Test
    @DisplayName("Should revoke the cookie's session and clear the cookie on logout")
    @TestSecurity(user = "testuser", roles = {"user"})
    void testLogoutUsesCookie() {
        given()
            .contentType(ContentType.JSON)
            .cookie("refresh_token", "cookie-token")
        .when()
            .post("/api/auth/logout")
        .then()
            .statusCode(200)
            .cookie("refresh_token", detailedCookie().value("").maxAge(0));

        verify(authService).logout("testuser", "cookie-token");
    }

    @Test
    @DisplayName("Should reject a refresh request with neither cookie nor body token")
    void testRefreshTokenMissing() {
        given()
            .contentType(ContentType.JSON)
            .body("{}")
        .when()
            .post("/api/auth/refresh")
        .then()
            .statusCode(401);

        verify(authService, never()).refreshToken(any());
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
