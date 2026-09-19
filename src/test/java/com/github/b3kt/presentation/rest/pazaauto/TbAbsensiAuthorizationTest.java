package com.github.b3kt.presentation.rest.pazaauto;

import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.application.service.pazaauto.TbAbsensiService;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbAbsensiEntity;
import com.github.b3kt.infrastructure.security.Roles;
import com.github.b3kt.integration.IntegrationTestBase;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.build.JwtClaimsBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Employees may only touch their own attendance; the karyawanId comes from their JWT, not the request.
 * Uses real signed tokens so the karyawanId claim is read the same way as in production.
 */
@QuarkusTest
@DisplayName("TbAbsensiResource authorization")
class TbAbsensiAuthorizationTest extends IntegrationTestBase {

    private static final long OWN_ID = 7L;
    private static final long OTHER_ID = 8L;

    @InjectMock
    TbAbsensiService service;

    @ConfigProperty(name = "mp.jwt.verify.issuer")
    String issuer;

    private TbAbsensiEntity ownRecord;

    @BeforeEach
    void stubService() {
        ownRecord = new TbAbsensiEntity();
        ownRecord.setId(1L);
        ownRecord.setKaryawanId(OWN_ID);
        when(service.clockIn(anyLong(), any(), any(), any())).thenReturn(ownRecord);
        when(service.clockOut(anyLong(), any(), any())).thenReturn(ownRecord);
        when(service.getTodayAttendance(anyLong())).thenReturn(ownRecord);
        when(service.getMonthlySummary(anyLong(), anyInt(), anyInt())).thenReturn(Map.of());
        when(service.getAttendanceHistory(any(), any(), any(), any(), any()))
                .thenReturn(new PageResponse<>(List.of(ownRecord), 1, 10, 1));
    }

    private String token(String role, Long karyawanId) {
        JwtClaimsBuilder builder = Jwt.issuer(issuer).upn("user").subject("user").groups(Set.of(role));
        if (karyawanId != null) {
            builder.claim("karyawanId", karyawanId);
        }
        return builder.sign();
    }

    private String employee() {
        return token(Roles.KARYAWAN, OWN_ID);
    }

    @Test
    @DisplayName("Employee clock-in uses the karyawanId from the token")
    void clockInOwn() {
        given().auth().oauth2(employee()).contentType(ContentType.JSON)
                .body(Map.of("karyawanId", OWN_ID))
                .when().post("/api/pazaauto/absensi/clock-in")
                .then().statusCode(200);
        verify(service).clockIn(eq(OWN_ID), any(), any(), any());

        // Omitting karyawanId still clocks in the caller
        given().auth().oauth2(employee()).contentType(ContentType.JSON)
                .body(Map.of())
                .when().post("/api/pazaauto/absensi/clock-in")
                .then().statusCode(200);
        verify(service, times(2)).clockIn(eq(OWN_ID), any(), any(), any());
    }

    @Test
    @DisplayName("Clock-in IP is the connection address, not a client-supplied header")
    void clockInIgnoresForwardedHeaders() {
        given().auth().oauth2(employee()).contentType(ContentType.JSON)
                .header("X-Forwarded-For", "203.0.113.10")
                .header("X-Real-IP", "203.0.113.11")
                .body(Map.of())
                .when().post("/api/pazaauto/absensi/clock-in")
                .then().statusCode(200);
        given().auth().oauth2(employee()).contentType(ContentType.JSON)
                .header("X-Forwarded-For", "203.0.113.10")
                .body(Map.of())
                .when().post("/api/pazaauto/absensi/clock-out")
                .then().statusCode(200);

        verify(service).clockIn(eq(OWN_ID), eq("127.0.0.1"), any(), any());
        verify(service).clockOut(eq(OWN_ID), eq("127.0.0.1"), any());
    }

    @Test
    @DisplayName("Employee cannot clock in or out for someone else")
    void clockForOtherForbidden() {
        given().auth().oauth2(employee()).contentType(ContentType.JSON)
                .body(Map.of("karyawanId", OTHER_ID))
                .when().post("/api/pazaauto/absensi/clock-in")
                .then().statusCode(403);
        given().auth().oauth2(employee()).contentType(ContentType.JSON)
                .body(Map.of("karyawanId", OTHER_ID))
                .when().post("/api/pazaauto/absensi/clock-out")
                .then().statusCode(403);
        verify(service, never()).clockIn(anyLong(), any(), any(), any());
        verify(service, never()).clockOut(anyLong(), any(), any());
    }

    @Test
    @DisplayName("Employee without a linked karyawan is rejected")
    void employeeWithoutKaryawanForbidden() {
        given().auth().oauth2(token(Roles.KARYAWAN, null)).contentType(ContentType.JSON)
                .body(Map.of("karyawanId", OWN_ID))
                .when().post("/api/pazaauto/absensi/clock-in")
                .then().statusCode(403);
    }

    @Test
    @DisplayName("Employee can read only their own today/summary/history/record")
    void readOwnOnly() {
        given().auth().oauth2(employee()).when().get("/api/pazaauto/absensi/today/" + OWN_ID).then().statusCode(200);
        given().auth().oauth2(employee()).when().get("/api/pazaauto/absensi/today/" + OTHER_ID).then().statusCode(403);

        given().auth().oauth2(employee()).when().get("/api/pazaauto/absensi/summary/" + OWN_ID + "?month=1&year=2026")
                .then().statusCode(200);
        given().auth().oauth2(employee()).when().get("/api/pazaauto/absensi/summary/" + OTHER_ID + "?month=1&year=2026")
                .then().statusCode(403);

        given().auth().oauth2(employee()).queryParam("karyawanId", OTHER_ID)
                .when().get("/api/pazaauto/absensi/history").then().statusCode(403);
        // Without a filter an employee still only gets their own history
        given().auth().oauth2(employee()).when().get("/api/pazaauto/absensi/history").then().statusCode(200);
        verify(service).getAttendanceHistory(eq(OWN_ID), any(), any(), any(), any());

        when(service.findById(1L)).thenReturn(ownRecord);
        given().auth().oauth2(employee()).when().get("/api/pazaauto/absensi/1").then().statusCode(200);
        TbAbsensiEntity otherRecord = new TbAbsensiEntity();
        otherRecord.setId(2L);
        otherRecord.setKaryawanId(OTHER_ID);
        when(service.findById(2L)).thenReturn(otherRecord);
        given().auth().oauth2(employee()).when().get("/api/pazaauto/absensi/2").then().statusCode(403);
    }

    @Test
    @DisplayName("Employee cannot list, create, edit, delete or mark absences")
    void managementEndpointsForbidden() {
        String body = "{\"karyawanId\": " + OWN_ID + ", \"tanggal\": \"2026-01-15\", \"status\": \"IZIN\"}";
        given().auth().oauth2(employee()).when().get("/api/pazaauto/absensi").then().statusCode(403);
        given().auth().oauth2(employee()).when().get("/api/pazaauto/absensi/paginated").then().statusCode(403);
        given().auth().oauth2(employee()).contentType(ContentType.JSON).body(body)
                .when().post("/api/pazaauto/absensi").then().statusCode(403);
        given().auth().oauth2(employee()).contentType(ContentType.JSON).body(body)
                .when().put("/api/pazaauto/absensi/1").then().statusCode(403);
        given().auth().oauth2(employee()).when().delete("/api/pazaauto/absensi/1").then().statusCode(403);
        given().auth().oauth2(employee()).contentType(ContentType.JSON).body(body)
                .when().post("/api/pazaauto/absensi/mark-absence").then().statusCode(403);

        verify(service, never()).create(any());
        verify(service, never()).update(anyLong(), any());
        verify(service, never()).delete(anyLong());
        verify(service, never()).markAbsence(anyLong(), any(), any(), any());
    }

    @Test
    @DisplayName("Admin can act on any employee")
    void adminActsOnAnyone() {
        String admin = token(Roles.ADMIN, null);
        given().auth().oauth2(admin).contentType(ContentType.JSON)
                .body(Map.of("karyawanId", OTHER_ID))
                .when().post("/api/pazaauto/absensi/clock-in")
                .then().statusCode(200);
        verify(service).clockIn(eq(OTHER_ID), any(), any(), any());

        given().auth().oauth2(admin).when().get("/api/pazaauto/absensi/today/" + OTHER_ID).then().statusCode(200);
        given().auth().oauth2(admin).when().get("/api/pazaauto/absensi/history").then().statusCode(200);
        verify(service).getAttendanceHistory(isNull(), any(), any(), any(), any());
    }
}
