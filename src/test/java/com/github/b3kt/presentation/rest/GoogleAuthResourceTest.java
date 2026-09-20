package com.github.b3kt.presentation.rest;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * Google sign-in is off unless configured, and while it is off it must have no reachable surface.
 */
@QuarkusTest
@Tag("quarkus")
class GoogleAuthResourceTest {

    @Test
    @DisplayName("GET /api/auth/google/start - 404 while the feature is disabled")
    void startIsNotFoundWhenDisabled() {
        given()
            .redirects().follow(false)
            .when().get("/api/auth/google/start")
            .then().statusCode(404);
    }

    @Test
    @DisplayName("GET /api/auth/google/callback - 404 while the feature is disabled")
    void callbackIsNotFoundWhenDisabled() {
        given()
            .redirects().follow(false)
            .queryParam("code", "any").queryParam("state", "any")
            .when().get("/api/auth/google/callback")
            .then().statusCode(404);
    }

    @Test
    @DisplayName("GET /api/auth/config - public, and reports the feature as off")
    void configIsPublicAndReportsDisabled() {
        given()
            .when().get("/api/auth/config")
            .then().statusCode(200)
            .body("success", equalTo(true))
            .body("data.googleLoginEnabled", equalTo(false));
    }
}
