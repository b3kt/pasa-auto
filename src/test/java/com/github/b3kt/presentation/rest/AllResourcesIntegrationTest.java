package com.github.b3kt.presentation.rest;

import com.github.b3kt.application.dto.*;
import com.github.b3kt.application.dto.pazaauto.PenjualanPrintDto;
import com.github.b3kt.application.dto.pazaauto.RekapPenjualanDto;
import com.github.b3kt.application.dto.pazaauto.SummaryDto;
import com.github.b3kt.application.service.*;
import com.github.b3kt.application.service.pazaauto.*;
import com.github.b3kt.domain.model.Permission;
import com.github.b3kt.domain.model.Role;
import com.github.b3kt.infrastructure.persistence.entity.AuditTrailEntity;
import com.github.b3kt.infrastructure.persistence.entity.PermissionEntity;
import com.github.b3kt.infrastructure.persistence.entity.RoleEntity;
import com.github.b3kt.infrastructure.persistence.entity.SystemParameterEntity;
import com.github.b3kt.infrastructure.persistence.entity.UserEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.*;
import com.github.b3kt.integration.IntegrationTestBase;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class AllResourcesIntegrationTest extends IntegrationTestBase {

    // ── Auth / Health ──────────────────────────────────────────────────────

    @InjectMock
    AuthService authService;

    @InjectMock
    AuditTrailService auditTrailService;

    @InjectMock
    RbacService rbacService;

    @InjectMock
    RoleService roleService;

    @InjectMock
    PermissionService permissionService;

    @InjectMock
    UserService userService;

    @InjectMock
    SystemParameterService systemParameterService;

    @InjectMock
    TbAbsensiService tbAbsensiService;

    @InjectMock
    TbBarangService tbBarangService;

    @InjectMock
    TbJasaService tbJasaService;

    @InjectMock
    TbKaryawanPosisiService tbKaryawanPosisiService;

    @InjectMock
    TbKaryawanService tbKaryawanService;

    @InjectMock
    TbKendaraanService tbKendaraanService;

    @InjectMock
    TbPelangganService tbPelangganService;

    @InjectMock
    TbPembelianDetailService tbPembelianDetailService;

    @InjectMock
    TbPembelianService tbPembelianService;

    @InjectMock
    TbPenjualanDetailService tbPenjualanDetailService;

    @InjectMock
    TbPenjualanService tbPenjualanService;

    @InjectMock
    TbSpkService tbSpkService;

    @InjectMock
    TbSparepartService tbSparepartService;

    @InjectMock
    TbSpkDetailService tbSpkDetailService;

    @InjectMock
    SummaryService summaryService;

    // ═══════════════════════════════════════════════════════════════════════
    //  HealthResource
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("GET /health returns 200")
    void health() {
        given()
            .when().get("/health")
            .then().statusCode(200)
            .body("success", equalTo(true))
            .body("data.status", equalTo("UP"))
            .body("data.application", equalTo("pasa-auto"));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  AuthResource — uncovered paths (AuthResourceTest covers the rest)
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("POST /api/auth/login returns 200")
    void authLogin() {
        LoginResponse resp = new LoginResponse("t", "r", "u", "e@x.com", 3600L);
        when(authService.login(anyString(), anyString())).thenReturn(resp);

        given()
            .contentType(ContentType.JSON).body(Map.of("username", "u", "password", "p"))
            .when().post("/api/auth/login")
            .then().statusCode(200).body("success", equalTo(true));
    }

    @Test
    @DisplayName("POST /api/auth/login returns 401 on bad credentials")
    void authLoginUnauthorized() {
        when(authService.login(anyString(), anyString())).thenThrow(new com.github.b3kt.domain.exception.AuthenticationException("bad"));

        given()
            .contentType(ContentType.JSON).body(Map.of("username", "u", "password", "p"))
            .when().post("/api/auth/login")
            .then().statusCode(401);
    }

    @Test
    @DisplayName("POST /api/auth/logout returns 200 when authenticated")
    @TestSecurity(user = "user", roles = {"user"})
    void authLogout() {
        given()
            .contentType(ContentType.JSON)
            .when().post("/api/auth/logout")
            .then().statusCode(200).body("success", equalTo(true));
    }

    @Test
    @DisplayName("POST /api/auth/logout returns 401 when unauthenticated")
    void authLogoutUnauthorized() {
        given()
            .contentType(ContentType.JSON)
            .when().post("/api/auth/logout")
            .then().statusCode(401);
    }

    @Test
    @DisplayName("GET /api/auth/me returns 401 when unauthenticated")
    void authMeUnauthorized() {
        given()
            .contentType(ContentType.JSON)
            .when().get("/api/auth/me")
            .then().statusCode(401);
    }

    @Test
    @DisplayName("GET /api/auth/me returns 200 when authenticated")
    @TestSecurity(user = "user", roles = {"user"})
    void authMe() {
        com.github.b3kt.application.dto.UserInfo info = new com.github.b3kt.application.dto.UserInfo();
        info.setUsername("u");
        when(authService.getUserInfo(any())).thenReturn(info);

        given()
            .contentType(ContentType.JSON)
            .when().get("/api/auth/me")
            .then().statusCode(200).body("success", equalTo(true));
    }

    @Test
    @DisplayName("POST /api/auth/refresh returns 200")
    void authRefresh() {
        LoginResponse resp = new LoginResponse("t", "r", "u", "e@x.com", 3600L);
        when(authService.refreshToken(anyString())).thenReturn(resp);

        given()
            .contentType(ContentType.JSON).body(Map.of("refreshToken", "rt"))
            .when().post("/api/auth/refresh")
            .then().statusCode(200);
    }

    @Test
    @DisplayName("POST /api/auth/refresh returns 401")
    void authRefreshUnauthorized() {
        when(authService.refreshToken(anyString())).thenThrow(new com.github.b3kt.domain.exception.AuthenticationException("bad"));

        given()
            .contentType(ContentType.JSON).body(Map.of("refreshToken", "rt"))
            .when().post("/api/auth/refresh")
            .then().statusCode(401);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  AuditTrailResource  (all require Admin role)
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("GET /api/audit-trail — Admin role required")
    @TestSecurity(user = "admin", roles = {"Admin"})
    void auditTrailList() {
        when(auditTrailService.findAll()).thenReturn(List.of());
        given()
            .when().get("/api/audit-trail")
            .then().statusCode(200).body("success", equalTo(true));
    }

    @Test
    @DisplayName("GET /api/audit-trail — 403 without Admin role")
    @TestSecurity(user = "user", roles = {"user"})
    void auditTrailListForbidden() {
        given()
            .when().get("/api/audit-trail")
            .then().statusCode(403);
    }

    @Test
    @DisplayName("GET /api/audit-trail/paginated")
    @TestSecurity(user = "admin", roles = {"Admin"})
    void auditTrailPaginated() {
        when(auditTrailService.findPaginated(any())).thenReturn(new PageResponse<>(List.of(), 1, 10, 0));
        given()
            .queryParam("page", 1).queryParam("rowsPerPage", 10)
            .when().get("/api/audit-trail/paginated")
            .then().statusCode(200);
    }

    @Test
    @DisplayName("GET /api/audit-trail/table/{tableName}")
    @TestSecurity(user = "admin", roles = {"Admin"})
    void auditTrailByTable() {
        when(auditTrailService.findByTableName("users")).thenReturn(List.of());
        given()
            .when().get("/api/audit-trail/table/users")
            .then().statusCode(200);
    }

    @Test
    @DisplayName("GET /api/audit-trail/record/{recordId}")
    @TestSecurity(user = "admin", roles = {"Admin"})
    void auditTrailByRecord() {
        when(auditTrailService.findByRecordId(1L)).thenReturn(List.of());
        given()
            .when().get("/api/audit-trail/record/1")
            .then().statusCode(200);
    }

    @Test
    @DisplayName("GET /api/audit-trail/table/{tableName}/record/{recordId}")
    @TestSecurity(user = "admin", roles = {"Admin"})
    void auditTrailByTableAndRecord() {
        when(auditTrailService.findByTableNameAndRecordId("users", 1L)).thenReturn(List.of());
        given()
            .when().get("/api/audit-trail/table/users/record/1")
            .then().statusCode(200);
    }

    @Test
    @DisplayName("GET /api/audit-trail/user/{userId}")
    @TestSecurity(user = "admin", roles = {"Admin"})
    void auditTrailByUser() {
        when(auditTrailService.findByUserId(1L)).thenReturn(List.of());
        given()
            .when().get("/api/audit-trail/user/1")
            .then().statusCode(200);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  RbacResource  (all methods require admin and rbac enabled)
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("POST /api/rbac/roles")
    @TestSecurity(user = "admin", roles = {"admin"})
    void rbacCreateRole() {
        when(rbacService.createRole(any(), any())).thenReturn(new Role());
        given()
            .contentType(ContentType.JSON).body(Map.of("name", "op"))
            .when().post("/api/rbac/roles")
            .then().statusCode(200);
    }

    @Test
    @DisplayName("POST /api/rbac/roles — duplicate name returns 400")
    @TestSecurity(user = "admin", roles = {"admin"})
    void rbacCreateRoleDuplicate() {
        when(rbacService.createRole(any(), any())).thenThrow(new IllegalArgumentException("already exists"));
        given()
            .contentType(ContentType.JSON).body(Map.of("name", "op"))
            .when().post("/api/rbac/roles")
            .then().statusCode(400);
    }

    @Test
    @DisplayName("GET /api/rbac/roles")
    @TestSecurity(user = "admin", roles = {"admin"})
    void rbacGetAllRoles() {
        when(rbacService.getAllRoles()).thenReturn(List.of());
        given()
            .when().get("/api/rbac/roles")
            .then().statusCode(200);
    }

    @Test
    @DisplayName("GET /api/rbac/roles/{id}")
    @TestSecurity(user = "admin", roles = {"admin"})
    void rbacGetRoleById() {
        when(rbacService.getRoleById(1L)).thenReturn(new Role());
        given()
            .when().get("/api/rbac/roles/1")
            .then().statusCode(200);
    }

    @Test
    @DisplayName("GET /api/rbac/roles/{id} — not found returns 404")
    @TestSecurity(user = "admin", roles = {"admin"})
    void rbacGetRoleByIdNotFound() {
        when(rbacService.getRoleById(1L)).thenThrow(new IllegalArgumentException("not found"));
        given()
            .when().get("/api/rbac/roles/1")
            .then().statusCode(404);
    }

    @Test
    @DisplayName("PUT /api/rbac/roles/{id}")
    @TestSecurity(user = "admin", roles = {"admin"})
    void rbacUpdateRole() {
        when(rbacService.updateRole(anyLong(), any(), any())).thenReturn(new Role());
        given()
            .contentType(ContentType.JSON).body(Map.of("name", "op"))
            .when().put("/api/rbac/roles/1")
            .then().statusCode(200);
    }

    @Test
    @DisplayName("DELETE /api/rbac/roles/{id}")
    @TestSecurity(user = "admin", roles = {"admin"})
    void rbacDeleteRole() {
        doNothing().when(rbacService).deleteRole(1L);
        given()
            .when().delete("/api/rbac/roles/1")
            .then().statusCode(200);
    }

    @Test
    @DisplayName("DELETE /api/rbac/roles/{id} — not found returns 404")
    @TestSecurity(user = "admin", roles = {"admin"})
    void rbacDeleteRoleNotFound() {
        doThrow(new IllegalArgumentException("not found")).when(rbacService).deleteRole(1L);
        given()
            .when().delete("/api/rbac/roles/1")
            .then().statusCode(404);
    }

    @Test
    @DisplayName("POST /api/rbac/roles/{id}/activate")
    @TestSecurity(user = "admin", roles = {"admin"})
    void rbacActivateRole() {
        doNothing().when(rbacService).activateRole(1L);
        given()
            .contentType(ContentType.JSON)
            .when().post("/api/rbac/roles/1/activate")
            .then().statusCode(200);
    }

    @Test
    @DisplayName("POST /api/rbac/roles/{id}/deactivate")
    @TestSecurity(user = "admin", roles = {"admin"})
    void rbacDeactivateRole() {
        doNothing().when(rbacService).deactivateRole(1L);
        given()
            .contentType(ContentType.JSON)
            .when().post("/api/rbac/roles/1/deactivate")
            .then().statusCode(200);
    }

    // -- Permissions

    @Test
    @DisplayName("POST /api/rbac/permissions")
    @TestSecurity(user = "admin", roles = {"admin"})
    void rbacCreatePermission() {
        when(rbacService.createPermission(any(), any(), any(), any())).thenReturn(new Permission());
        given()
            .contentType(ContentType.JSON).body(Map.of("name", "p", "resource", "r", "action", "a"))
            .when().post("/api/rbac/permissions")
            .then().statusCode(200);
    }

    @Test
    @DisplayName("POST /api/rbac/permissions — duplicate 400")
    @TestSecurity(user = "admin", roles = {"admin"})
    void rbacCreatePermissionDuplicate() {
        when(rbacService.createPermission(any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("already exists"));
        given()
            .contentType(ContentType.JSON).body(Map.of("name", "p", "resource", "r", "action", "a"))
            .when().post("/api/rbac/permissions")
            .then().statusCode(400);
    }

    @Test
    @DisplayName("GET /api/rbac/permissions")
    @TestSecurity(user = "admin", roles = {"admin"})
    void rbacGetAllPermissions() {
        when(rbacService.getAllPermissions()).thenReturn(List.of());
        given()
            .when().get("/api/rbac/permissions")
            .then().statusCode(200);
    }

    @Test
    @DisplayName("GET /api/rbac/permissions/{id}")
    @TestSecurity(user = "admin", roles = {"admin"})
    void rbacGetPermissionById() {
        when(rbacService.getPermissionById(1L)).thenReturn(new Permission());
        given()
            .when().get("/api/rbac/permissions/1")
            .then().statusCode(200);
    }

    @Test
    @DisplayName("GET /api/rbac/permissions/{id} — not found 404")
    @TestSecurity(user = "admin", roles = {"admin"})
    void rbacGetPermissionByIdNotFound() {
        when(rbacService.getPermissionById(1L)).thenThrow(new IllegalArgumentException("not found"));
        given()
            .when().get("/api/rbac/permissions/1")
            .then().statusCode(404);
    }

    // -- Role-Permission

    @Test
    @DisplayName("POST /api/rbac/roles/{roleId}/permissions/{permId}")
    @TestSecurity(user = "admin", roles = {"admin"})
    void rbacAssignPermission() {
        doNothing().when(rbacService).assignPermissionToRole(1L, 2L);
        given()
            .contentType(ContentType.JSON)
            .when().post("/api/rbac/roles/1/permissions/2")
            .then().statusCode(200);
    }

    @Test
    @DisplayName("DELETE /api/rbac/roles/{roleId}/permissions/{permId}")
    @TestSecurity(user = "admin", roles = {"admin"})
    void rbacRemovePermission() {
        doNothing().when(rbacService).removePermissionFromRole(1L, 2L);
        given()
            .when().delete("/api/rbac/roles/1/permissions/2")
            .then().statusCode(200);
    }

    @Test
    @DisplayName("GET /api/rbac/roles/{roleId}/permissions")
    @TestSecurity(user = "admin", roles = {"admin"})
    void rbacGetRolePermissions() {
        when(rbacService.getRolePermissions(1L)).thenReturn(Set.of());
        given()
            .when().get("/api/rbac/roles/1/permissions")
            .then().statusCode(200);
    }

    // -- User-Role

    @Test
    @DisplayName("POST /api/rbac/users/{username}/roles/{roleId}")
    @TestSecurity(user = "admin", roles = {"admin"})
    void rbacAssignRoleToUser() {
        doNothing().when(rbacService).assignRoleToUser("u", 1L);
        given()
            .contentType(ContentType.JSON)
            .when().post("/api/rbac/users/u/roles/1")
            .then().statusCode(200);
    }

    @Test
    @DisplayName("DELETE /api/rbac/users/{username}/roles/{roleId}")
    @TestSecurity(user = "admin", roles = {"admin"})
    void rbacRemoveRoleFromUser() {
        doNothing().when(rbacService).removeRoleFromUser("u", 1L);
        given()
            .when().delete("/api/rbac/users/u/roles/1")
            .then().statusCode(200);
    }

    @Test
    @DisplayName("GET /api/rbac/users/{username}/roles")
    @TestSecurity(user = "admin", roles = {"admin"})
    void rbacGetUserRoles() {
        when(rbacService.getUserRoles("u")).thenReturn(Set.of());
        given()
            .when().get("/api/rbac/users/u/roles")
            .then().statusCode(200);
    }

    @Test
    @DisplayName("GET /api/rbac/users/{username}/permissions")
    @TestSecurity(user = "admin", roles = {"admin"})
    void rbacGetUserPermissions() {
        when(rbacService.getUserPermissions("u")).thenReturn(Set.of());
        given()
            .when().get("/api/rbac/users/u/permissions")
            .then().statusCode(200);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  RoleResource — CRUD + custom (Owner role)
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("GET /api/roles — requires Owner")
    @TestSecurity(user = "owner", roles = {"Owner"})
    void roleList() {
        when(roleService.findAll()).thenReturn(List.of());
        given()
            .when().get("/api/roles")
            .then().statusCode(200);
    }

    @Test
    @DisplayName("GET /api/roles/paginated")
    @TestSecurity(user = "owner", roles = {"Owner"})
    void rolePaginated() {
        when(roleService.findPaginated(any())).thenReturn(new PageResponse<>(List.of(), 1, 10, 0));
        given()
            .when().get("/api/roles/paginated?page=1&rowsPerPage=10")
            .then().statusCode(200);
    }

    @Test
    @DisplayName("GET /api/roles/{id}")
    @TestSecurity(user = "owner", roles = {"Owner"})
    void roleGetById() {
        when(roleService.findById(1L)).thenReturn(new RoleEntity());
        given()
            .when().get("/api/roles/1")
            .then().statusCode(200);
    }

    @Test
    @DisplayName("POST /api/roles")
    @TestSecurity(user = "owner", roles = {"Owner"})
    void roleCreate() {
        when(roleService.create(any())).thenReturn(new RoleEntity());
        given()
            .contentType(ContentType.JSON).body(Map.of("name", "r"))
            .when().post("/api/roles")
            .then().statusCode(200);
    }

    @Test
    @DisplayName("PUT /api/roles/{id}")
    @TestSecurity(user = "owner", roles = {"Owner"})
    void roleUpdate() {
        when(roleService.update(anyLong(), any())).thenReturn(new RoleEntity());
        given()
            .contentType(ContentType.JSON).body(Map.of("name", "r"))
            .when().put("/api/roles/1")
            .then().statusCode(200);
    }

    @Test
    @DisplayName("DELETE /api/roles/{id}")
    @TestSecurity(user = "owner", roles = {"Owner"})
    void roleDelete() {
        doNothing().when(roleService).delete(1L);
        given()
            .when().delete("/api/roles/1")
            .then().statusCode(200);
    }

    @Test
    @DisplayName("GET /api/roles/{id}/users")
    @TestSecurity(user = "owner", roles = {"Owner"})
    void roleGetUsers() {
        when(roleService.getUsersByRoleId(1L)).thenReturn(List.of());
        given()
            .when().get("/api/roles/1/users")
            .then().statusCode(200);
    }

    @Test
    @DisplayName("PUT /api/roles/{id}/users")
    @TestSecurity(user = "owner", roles = {"Owner"})
    void roleUpdateUsers() {
        doNothing().when(roleService).updateRoleUsers(anyLong(), anyList());
        given()
            .contentType(ContentType.JSON).body(Map.of("userIds", List.of(1, 2)))
            .when().put("/api/roles/1/users")
            .then().statusCode(200);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  PermissionResource — CRUD (PermissionEntity)
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("GET /api/permissions")
    void permissionList() {
        when(permissionService.findAll()).thenReturn(List.of());
        given()
            .when().get("/api/permissions")
            .then().statusCode(200);
    }

    @Test
    @DisplayName("GET /api/permissions/paginated")
    void permissionPaginated() {
        when(permissionService.findPaginated(any())).thenReturn(new PageResponse<>(List.of(), 1, 10, 0));
        given()
            .when().get("/api/permissions/paginated")
            .then().statusCode(200);
    }

    @Test
    @DisplayName("GET/POST/PUT/DELETE /api/permissions")
    void permissionCrud() {
        when(permissionService.findById(1L)).thenReturn(new PermissionEntity());
        given().when().get("/api/permissions/1").then().statusCode(200);

        when(permissionService.create(any())).thenReturn(new PermissionEntity());
        given().contentType(ContentType.JSON).body(Map.of("name", "p", "resource", "r", "action", "a"))
            .when().post("/api/permissions").then().statusCode(200);

        when(permissionService.update(anyLong(), any())).thenReturn(new PermissionEntity());
        given().contentType(ContentType.JSON).body(Map.of("name", "p"))
            .when().put("/api/permissions/1").then().statusCode(200);

        doNothing().when(permissionService).delete(1L);
        given().when().delete("/api/permissions/1").then().statusCode(200);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  UserResource — CRUD (Owner role)
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("GET /api/users — requires Owner")
    @TestSecurity(user = "owner", roles = {"Owner"})
    void userList() {
        when(userService.findAll()).thenReturn(List.of());
        given().when().get("/api/users").then().statusCode(200);
    }

    @Test
    @DisplayName("GET /api/users/paginated")
    @TestSecurity(user = "owner", roles = {"Owner"})
    void userPaginated() {
        when(userService.findPaginated(any())).thenReturn(new PageResponse<>(List.of(), 1, 10, 0));
        given().when().get("/api/users/paginated").then().statusCode(200);
    }

    @Test
    @DisplayName("GET /api/users/{id}")
    @TestSecurity(user = "owner", roles = {"Owner"})
    void userGetById() {
        when(userService.findById(1L)).thenReturn(new UserEntity());
        given().when().get("/api/users/1").then().statusCode(200);
    }

    @Test
    @DisplayName("POST /api/users")
    @TestSecurity(user = "owner", roles = {"Owner"})
    void userCreate() {
        when(userService.create(any())).thenReturn(new UserEntity());
        given().contentType(ContentType.JSON).body(Map.of("username", "u"))
            .when().post("/api/users").then().statusCode(200);
    }

    @Test
    @DisplayName("PUT /api/users/{id}")
    @TestSecurity(user = "owner", roles = {"Owner"})
    void userUpdate() {
        when(userService.update(anyLong(), any())).thenReturn(new UserEntity());
        given().contentType(ContentType.JSON).body(Map.of("username", "u"))
            .when().put("/api/users/1").then().statusCode(200);
    }

    @Test
    @DisplayName("DELETE /api/users/{id}")
    @TestSecurity(user = "owner", roles = {"Owner"})
    void userDelete() {
        doNothing().when(userService).delete(1L);
        given().when().delete("/api/users/1").then().statusCode(200);
    }

    @Test
    @DisplayName("GET /api/users — 403 without Owner")
    @TestSecurity(user = "user", roles = {"user"})
    void userForbidden() {
        given().when().get("/api/users").then().statusCode(403);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  SystemParameterResource — CRUD
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("SystemParameter CRUD")
    void systemParamCrud() {
        when(systemParameterService.findAll()).thenReturn(List.of());
        given().when().get("/api/system-parameters").then().statusCode(200);

        when(systemParameterService.findPaginated(any())).thenReturn(new PageResponse<>(List.of(), 1, 10, 0));
        given().when().get("/api/system-parameters/paginated").then().statusCode(200);

        when(systemParameterService.findById(1L)).thenReturn(new SystemParameterEntity());
        given().when().get("/api/system-parameters/1").then().statusCode(200);

        when(systemParameterService.create(any())).thenReturn(new SystemParameterEntity());
        given().contentType(ContentType.JSON).body(Map.of("name", "k", "value", "v"))
            .when().post("/api/system-parameters").then().statusCode(200);

        when(systemParameterService.update(anyLong(), any())).thenReturn(new SystemParameterEntity());
        given().contentType(ContentType.JSON).body(Map.of("value", "v2"))
            .when().put("/api/system-parameters/1").then().statusCode(200);

        doNothing().when(systemParameterService).delete(1L);
        given().when().delete("/api/system-parameters/1").then().statusCode(200);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  TbAbsensiResource — custom + inherited CRUD
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("TbAbsensi CRUD + custom endpoints")
    void absensiFull() {
        TbAbsensiEntity absen = new TbAbsensiEntity();
        absen.setId(1L);

        // inherited CRUD
        when(tbAbsensiService.findAll()).thenReturn(List.of(absen));
        given().when().get("/api/pazaauto/absensi").then().statusCode(200);

        when(tbAbsensiService.findPaginated(any())).thenReturn(new PageResponse<>(List.of(absen), 1, 10, 1));
        given().when().get("/api/pazaauto/absensi/paginated").then().statusCode(200);

        when(tbAbsensiService.findById(1L)).thenReturn(absen);
        given().when().get("/api/pazaauto/absensi/1").then().statusCode(200);

        when(tbAbsensiService.create(any())).thenReturn(absen);
        given().contentType(ContentType.JSON).body(Map.of("karyawanId", 1))
            .when().post("/api/pazaauto/absensi").then().statusCode(200);

        when(tbAbsensiService.update(anyLong(), any())).thenReturn(absen);
        given().contentType(ContentType.JSON).body(Map.of("status", "HADIR"))
            .when().put("/api/pazaauto/absensi/1").then().statusCode(200);

        doNothing().when(tbAbsensiService).delete(1L);
        given().when().delete("/api/pazaauto/absensi/1").then().statusCode(200);

        // clock-in success
        when(tbAbsensiService.clockIn(anyLong(), any(), any(), any())).thenReturn(absen);
        given().contentType(ContentType.JSON).body(Map.of("karyawanId", 1, "location", "offc", "deviceInfo", "d"))
            .when().post("/api/pazaauto/absensi/clock-in")
            .then().statusCode(200);

        // clock-in error
        when(tbAbsensiService.clockIn(anyLong(), any(), any(), any()))
            .thenThrow(new IllegalStateException("already clocked in"));
        given().contentType(ContentType.JSON).body(Map.of("karyawanId", 1))
            .when().post("/api/pazaauto/absensi/clock-in")
            .then().statusCode(400);

        // clock-out
        when(tbAbsensiService.clockOut(anyLong(), any(), any())).thenReturn(absen);
        given().contentType(ContentType.JSON).body(Map.of("karyawanId", 1, "location", "offc"))
            .when().post("/api/pazaauto/absensi/clock-out")
            .then().statusCode(200);

        when(tbAbsensiService.clockOut(anyLong(), any(), any()))
            .thenThrow(new IllegalStateException("must clock in first"));
        given().contentType(ContentType.JSON).body(Map.of("karyawanId", 1))
            .when().post("/api/pazaauto/absensi/clock-out")
            .then().statusCode(400);

        // today
        when(tbAbsensiService.getTodayAttendance(1L)).thenReturn(absen);
        given().when().get("/api/pazaauto/absensi/today/1").then().statusCode(200);

        // history
        when(tbAbsensiService.getAttendanceHistory(anyLong(), any(), any(), anyString(), any()))
            .thenReturn(new PageResponse<>(List.of(absen), 1, 10, 1));
        given().queryParam("karyawanId", 1).queryParam("startDate", "2025-01-01")
            .when().get("/api/pazaauto/absensi/history")
            .then().statusCode(200);

        // summary
        when(tbAbsensiService.getMonthlySummary(1L, 1, 2025)).thenReturn(Map.of());
        given().when().get("/api/pazaauto/absensi/summary/1?month=1&year=2025")
            .then().statusCode(200);

        // mark-absence
        when(tbAbsensiService.markAbsence(anyLong(), any(), anyString(), anyString())).thenReturn(absen);
        given().contentType(ContentType.JSON)
            .body(Map.of("karyawanId", 1, "tanggal", "2025-01-15", "status", "IZIN", "keterangan", "sakit"))
            .when().post("/api/pazaauto/absensi/mark-absence")
            .then().statusCode(200);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  TbBarangResource — CRUD + search override
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("TbBarang CRUD + search")
    void barangCrud() {
        TbBarangEntity b = new TbBarangEntity();
        b.setId(1L);

        when(tbBarangService.findAll()).thenReturn(List.of(b));
        given().when().get("/api/pazaauto/barang").then().statusCode(200);

        when(tbBarangService.search("oli")).thenReturn(List.of(b));
        given().queryParam("search", "oli").when().get("/api/pazaauto/barang").then().statusCode(200);

        when(tbBarangService.findPaginated(any())).thenReturn(new PageResponse<>(List.of(b), 1, 10, 1));
        given().when().get("/api/pazaauto/barang/paginated").then().statusCode(200);

        when(tbBarangService.findById(1L)).thenReturn(b);
        given().when().get("/api/pazaauto/barang/1").then().statusCode(200);

        when(tbBarangService.create(any())).thenReturn(b);
        given().contentType(ContentType.JSON).body(Map.of("namaBarang", "oli"))
            .when().post("/api/pazaauto/barang").then().statusCode(200);

        when(tbBarangService.update(anyLong(), any())).thenReturn(b);
        given().contentType(ContentType.JSON).body(Map.of("namaBarang", "oli"))
            .when().put("/api/pazaauto/barang/1").then().statusCode(200);

        doNothing().when(tbBarangService).delete(1L);
        given().when().delete("/api/pazaauto/barang/1").then().statusCode(200);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  TbJasaResource — CRUD
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("TbJasa CRUD")
    void jasaCrud() {
        TbJasaEntity j = new TbJasaEntity();
        j.setId(1L);

        when(tbJasaService.findAll()).thenReturn(List.of(j));
        given().when().get("/api/pazaauto/jasa").then().statusCode(200);

        when(tbJasaService.findPaginated(any())).thenReturn(new PageResponse<>(List.of(j), 1, 10, 1));
        given().when().get("/api/pazaauto/jasa/paginated").then().statusCode(200);

        when(tbJasaService.findById(1L)).thenReturn(j);
        given().when().get("/api/pazaauto/jasa/1").then().statusCode(200);

        when(tbJasaService.create(any())).thenReturn(j);
        given().contentType(ContentType.JSON).body(Map.of("namaJasa", "cuci"))
            .when().post("/api/pazaauto/jasa").then().statusCode(200);

        when(tbJasaService.update(anyLong(), any())).thenReturn(j);
        given().contentType(ContentType.JSON).body(Map.of("hargaJasa", 50000))
            .when().put("/api/pazaauto/jasa/1").then().statusCode(200);

        doNothing().when(tbJasaService).delete(1L);
        given().when().delete("/api/pazaauto/jasa/1").then().statusCode(200);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  TbKaryawanPosisiResource — CRUD
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("TbKaryawanPosisi CRUD")
    void karyawanPosisiCrud() {
        TbKaryawanPosisiEntity p = new TbKaryawanPosisiEntity();
        p.setId(1L);

        when(tbKaryawanPosisiService.findAll()).thenReturn(List.of(p));
        given().when().get("/api/pazaauto/karyawan-posisi").then().statusCode(200);

        when(tbKaryawanPosisiService.findPaginated(any())).thenReturn(new PageResponse<>(List.of(p), 1, 10, 1));
        given().when().get("/api/pazaauto/karyawan-posisi/paginated").then().statusCode(200);

        when(tbKaryawanPosisiService.findById(1L)).thenReturn(p);
        given().when().get("/api/pazaauto/karyawan-posisi/1").then().statusCode(200);

        when(tbKaryawanPosisiService.create(any())).thenReturn(p);
        given().contentType(ContentType.JSON).body(Map.of("posisi", "montir"))
            .when().post("/api/pazaauto/karyawan-posisi").then().statusCode(200);

        when(tbKaryawanPosisiService.update(anyLong(), any())).thenReturn(p);
        given().contentType(ContentType.JSON).body(Map.of("keterangan", "x"))
            .when().put("/api/pazaauto/karyawan-posisi/1").then().statusCode(200);

        doNothing().when(tbKaryawanPosisiService).delete(1L);
        given().when().delete("/api/pazaauto/karyawan-posisi/1").then().statusCode(200);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  TbKaryawanResource — CRUD + /unregistered
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("TbKaryawan CRUD + unregistered")
    void karyawanCrud() {
        TbKaryawanEntity k = new TbKaryawanEntity();
        k.setId(1L);

        when(tbKaryawanService.findAll()).thenReturn(List.of(k));
        given().when().get("/api/pazaauto/karyawan").then().statusCode(200);

        when(tbKaryawanService.findPaginated(any())).thenReturn(new PageResponse<>(List.of(k), 1, 10, 1));
        given().when().get("/api/pazaauto/karyawan/paginated").then().statusCode(200);

        when(tbKaryawanService.findById(1L)).thenReturn(k);
        given().when().get("/api/pazaauto/karyawan/1").then().statusCode(200);

        when(tbKaryawanService.create(any())).thenReturn(k);
        given().contentType(ContentType.JSON).body(Map.of("namaKaryawan", "budi"))
            .when().post("/api/pazaauto/karyawan").then().statusCode(200);

        when(tbKaryawanService.update(anyLong(), any())).thenReturn(k);
        given().contentType(ContentType.JSON).body(Map.of("email", "b@x.com"))
            .when().put("/api/pazaauto/karyawan/1").then().statusCode(200);

        doNothing().when(tbKaryawanService).delete(1L);
        given().when().delete("/api/pazaauto/karyawan/1").then().statusCode(200);

        when(tbKaryawanService.findAllUnregistered()).thenReturn(List.of(k));
        given().when().get("/api/pazaauto/karyawan/unregistered").then().statusCode(200);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  TbKendaraanResource — CRUD + distinct endpoints
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("TbKendaraan CRUD + distinct")
    void kendaraanFull() {
        TbKendaraanEntity k = new TbKendaraanEntity();
        k.setId(1L);

        when(tbKendaraanService.findAll()).thenReturn(List.of(k));
        given().when().get("/api/pazaauto/kendaraan").then().statusCode(200);

        when(tbKendaraanService.findPaginated(any())).thenReturn(new PageResponse<>(List.of(k), 1, 10, 1));
        given().when().get("/api/pazaauto/kendaraan/paginated").then().statusCode(200);

        when(tbKendaraanService.findById(1L)).thenReturn(k);
        given().when().get("/api/pazaauto/kendaraan/1").then().statusCode(200);

        when(tbKendaraanService.create(any())).thenReturn(k);
        given().contentType(ContentType.JSON).body(Map.of("merk", "honda"))
            .when().post("/api/pazaauto/kendaraan").then().statusCode(200);

        when(tbKendaraanService.update(anyLong(), any())).thenReturn(k);
        given().contentType(ContentType.JSON).body(Map.of("jenis", "mobil"))
            .when().put("/api/pazaauto/kendaraan/1").then().statusCode(200);

        doNothing().when(tbKendaraanService).delete(1L);
        given().when().delete("/api/pazaauto/kendaraan/1").then().statusCode(200);

        // distinct
        when(tbKendaraanService.findDistinctMerks()).thenReturn(List.of("honda"));
        given().when().get("/api/pazaauto/kendaraan/merk/distinct").then().statusCode(200);

        when(tbKendaraanService.findDistinctJenis()).thenReturn(List.of("mobil"));
        given().when().get("/api/pazaauto/kendaraan/jenis/distinct").then().statusCode(200);

        when(tbKendaraanService.findDistinctJenisByMerk("honda")).thenReturn(List.of("mobil"));
        given().queryParam("merk", "honda").when().get("/api/pazaauto/kendaraan/jenis/by-merk").then().statusCode(200);

        // without merk param
        when(tbKendaraanService.findDistinctJenis()).thenReturn(List.of("mobil"));
        given().when().get("/api/pazaauto/kendaraan/jenis/by-merk").then().statusCode(200);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  TbPelangganResource — CRUD + by-nopol
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("TbPelanggan CRUD + by-nopol")
    void pelangganFull() {
        TbPelangganEntity p = new TbPelangganEntity();
        p.setId(1L);

        when(tbPelangganService.findAll()).thenReturn(List.of(p));
        given().when().get("/api/pazaauto/pelanggan").then().statusCode(200);

        when(tbPelangganService.findPaginated(any())).thenReturn(new PageResponse<>(List.of(p), 1, 10, 1));
        given().when().get("/api/pazaauto/pelanggan/paginated").then().statusCode(200);

        when(tbPelangganService.findById(1L)).thenReturn(p);
        given().when().get("/api/pazaauto/pelanggan/1").then().statusCode(200);

        when(tbPelangganService.create(any())).thenReturn(p);
        given().contentType(ContentType.JSON).body(Map.of("namaPelanggan", "budi"))
            .when().post("/api/pazaauto/pelanggan").then().statusCode(200);

        when(tbPelangganService.update(anyLong(), any())).thenReturn(p);
        given().contentType(ContentType.JSON).body(Map.of("telepon", "0811"))
            .when().put("/api/pazaauto/pelanggan/1").then().statusCode(200);

        doNothing().when(tbPelangganService).delete(1L);
        given().when().delete("/api/pazaauto/pelanggan/1").then().statusCode(200);

        // by-nopol found
        when(tbPelangganService.findByNopol("B1234CD")).thenReturn(p);
        given().when().get("/api/pazaauto/pelanggan/by-nopol/B1234CD").then().statusCode(200);

        // by-nopol not found
        when(tbPelangganService.findByNopol("MISSING")).thenReturn(null);
        given().when().get("/api/pazaauto/pelanggan/by-nopol/MISSING").then().statusCode(200)
            .body("error", containsString("not found"));

        // put by-nopol found
        when(tbPelangganService.patchByNopol(anyString(), any())).thenReturn(p);
        given().contentType(ContentType.JSON).body(Map.of("namaPelanggan", "x"))
            .when().put("/api/pazaauto/pelanggan/by-nopol/B1234CD").then().statusCode(200);

        // put by-nopol not found
        when(tbPelangganService.patchByNopol(eq("MISSING"), any())).thenReturn(null);
        given().contentType(ContentType.JSON).body(Map.of("namaPelanggan", "x"))
            .when().put("/api/pazaauto/pelanggan/by-nopol/MISSING").then().statusCode(200)
            .body("error", containsString("not found"));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  TbPembelianDetailResource — CRUD + by-pembelian
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("TbPembelianDetail CRUD + by-pembelian")
    void pembelianDetailFull() {
        TbPembelianDetailEntity d = new TbPembelianDetailEntity();
        d.setId(1L);

        when(tbPembelianDetailService.findAll()).thenReturn(List.of(d));
        given().when().get("/api/pazaauto/pembelian-detail").then().statusCode(200);

        when(tbPembelianDetailService.findPaginated(any())).thenReturn(new PageResponse<>(List.of(d), 1, 10, 1));
        given().when().get("/api/pazaauto/pembelian-detail/paginated").then().statusCode(200);

        when(tbPembelianDetailService.findById(1L)).thenReturn(d);
        given().when().get("/api/pazaauto/pembelian-detail/1").then().statusCode(200);

        when(tbPembelianDetailService.create(any())).thenReturn(d);
        given().contentType(ContentType.JSON).body(Map.of("namaItem", "oli"))
            .when().post("/api/pazaauto/pembelian-detail").then().statusCode(200);

        when(tbPembelianDetailService.update(anyLong(), any())).thenReturn(d);
        given().contentType(ContentType.JSON).body(Map.of("harga", 10000))
            .when().put("/api/pazaauto/pembelian-detail/1").then().statusCode(200);

        doNothing().when(tbPembelianDetailService).delete(1L);
        given().when().delete("/api/pazaauto/pembelian-detail/1").then().statusCode(200);

        // by-pembelian
        when(tbPembelianDetailService.findByPembelianId(1L)).thenReturn(List.of(d));
        given().when().get("/api/pazaauto/pembelian-detail/by-pembelian/1").then().statusCode(200);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  TbPembelianResource — CRUD + custom endpoints
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("TbPembelian full")
    void pembelianFull() {
        TbPembelianEntity p = new TbPembelianEntity();
        p.setId(1L);
        p.setNoPembelian("FS20250101001");

        // inherited list
        when(tbPembelianService.findAll()).thenReturn(List.of(p));
        given().when().get("/api/pazaauto/pembelian").then().statusCode(200);

        // custom paginated
        when(tbPembelianService.findPaginated(any())).thenReturn(new PageResponse<>(List.of(p), 1, 10, 1));
        given().queryParam("jenisPembelian", "SPAREPART").queryParam("kategoriOperasional", "OP")
            .when().get("/api/pazaauto/pembelian/paginated")
            .then().statusCode(200);

        // get by noPembelian (overrides parent getById)
        when(tbPembelianService.findByNoPembelian("FS20250101001")).thenReturn(p);
        when(tbPembelianDetailService.findByPembelianId(1L)).thenReturn(List.of());
        given().when().get("/api/pazaauto/pembelian/FS20250101001").then().statusCode(200);

        // get by noPembelian — not found
        when(tbPembelianService.findByNoPembelian("MISSING")).thenReturn(null);
        given().when().get("/api/pazaauto/pembelian/MISSING").then().statusCode(404);

        // create
        when(tbPembelianService.create(any())).thenReturn(p);
        given().contentType(ContentType.JSON).body(Map.of("jenisPembelian", "SPAREPART"))
            .when().post("/api/pazaauto/pembelian").then().statusCode(200);

        // create with details
        when(tbPembelianService.createWithDetails(any(), anyList())).thenReturn(p);
        given().contentType(ContentType.JSON)
            .body(Map.of("pembelian", Map.of("jenisPembelian", "SPAREPART"), "details", List.of()))
            .when().post("/api/pazaauto/pembelian/with-details")
            .then().statusCode(200);

        // update
        when(tbPembelianService.update(anyLong(), any())).thenReturn(p);
        given().contentType(ContentType.JSON).body(Map.of("grandTotal", 10000))
            .when().put("/api/pazaauto/pembelian/1").then().statusCode(200);

        // update with details
        when(tbPembelianService.updateWithDetails(anyLong(), any(), anyList())).thenReturn(p);
        given().contentType(ContentType.JSON)
            .body(Map.of("pembelian", Map.of(), "details", List.of()))
            .when().put("/api/pazaauto/pembelian/1/with-details")
            .then().statusCode(200);

        // get-next-number
        when(tbPembelianService.generateNoPembelian("SPAREPART")).thenReturn("FS20250101002");
        given().queryParam("jenisPembelian", "SPAREPART")
            .when().get("/api/pazaauto/pembelian/get-next-number")
            .then().statusCode(200);

        // generate-no
        when(tbPembelianService.generateNoPembelian("BARANG")).thenReturn("FB20250101001");
        given().queryParam("jenisPembelian", "BARANG")
            .when().get("/api/pazaauto/pembelian/generate-no")
            .then().statusCode(200);

        // delete
        doNothing().when(tbPembelianService).delete(1L);
        given().when().delete("/api/pazaauto/pembelian/1").then().statusCode(200);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  TbPenjualanDetailResource — CRUD
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("TbPenjualanDetail CRUD")
    void penjualanDetailCrud() {
        TbPenjualanDetailEntity d = new TbPenjualanDetailEntity();
        d.setId(1L);

        when(tbPenjualanDetailService.findAll()).thenReturn(List.of(d));
        given().when().get("/api/pazaauto/penjualan-detail").then().statusCode(200);

        when(tbPenjualanDetailService.findPaginated(any())).thenReturn(new PageResponse<>(List.of(d), 1, 10, 1));
        given().when().get("/api/pazaauto/penjualan-detail/paginated").then().statusCode(200);

        when(tbPenjualanDetailService.findById(1L)).thenReturn(d);
        given().when().get("/api/pazaauto/penjualan-detail/1").then().statusCode(200);

        when(tbPenjualanDetailService.create(any())).thenReturn(d);
        given().contentType(ContentType.JSON).body(Map.of("kategori", "JASA"))
            .when().post("/api/pazaauto/penjualan-detail").then().statusCode(200);

        when(tbPenjualanDetailService.update(anyLong(), any())).thenReturn(d);
        given().contentType(ContentType.JSON).body(Map.of("kuantiti", 2))
            .when().put("/api/pazaauto/penjualan-detail/1").then().statusCode(200);

        doNothing().when(tbPenjualanDetailService).delete(1L);
        given().when().delete("/api/pazaauto/penjualan-detail/1").then().statusCode(200);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  TbPenjualanResource — CRUD (String ID) + print + cancel-by-no-spk
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("TbPenjualan full")
    void penjualanFull() {
        TbPenjualanEntity p = new TbPenjualanEntity();
        p.setNoPenjualan("PJ20250101001");

        // list
        when(tbPenjualanService.findAll()).thenReturn(List.of(p));
        given().when().get("/api/pazaauto/penjualan").then().statusCode(200);

        // paginated
        when(tbPenjualanService.findPaginated(any())).thenReturn(new PageResponse<>(List.of(p), 1, 10, 1));
        given().when().get("/api/pazaauto/penjualan/paginated").then().statusCode(200);

        // get by id (noPenjualan)
        when(tbPenjualanService.findByNoPenjualan("PJ20250101001")).thenReturn(p);
        given().when().get("/api/pazaauto/penjualan/PJ20250101001").then().statusCode(200);

        // get by id not found
        when(tbPenjualanService.findByNoPenjualan("MISSING")).thenReturn(null);
        given().when().get("/api/pazaauto/penjualan/MISSING").then().statusCode(404);

        // create
        when(tbPenjualanService.createWithNoSpkValidation(any())).thenReturn(p);
        given().contentType(ContentType.JSON).body(Map.of("noPenjualan", "PJ001"))
            .when().post("/api/pazaauto/penjualan").then().statusCode(200);

        // update
        when(tbPenjualanService.updateWithNoSpkValidation(any())).thenReturn(p);
        given().contentType(ContentType.JSON).body(Map.of("noPenjualan", "PJ001"))
            .when().put("/api/pazaauto/penjualan/PJ001").then().statusCode(200);

        // delete
        doNothing().when(tbPenjualanService).delete(anyString());
        given().when().delete("/api/pazaauto/penjualan/PJ001").then().statusCode(200);

        // print found
        PenjualanPrintDto dto = new PenjualanPrintDto();
        dto.setNoPenjualan("PJ001");
        when(tbPenjualanService.buildPrintDto("PJ001")).thenReturn(dto);
        given().when().get("/api/pazaauto/penjualan/PJ001/print").then().statusCode(200);

        // print not found
        when(tbPenjualanService.buildPrintDto("MISS")).thenReturn(null);
        given().when().get("/api/pazaauto/penjualan/MISS/print").then().statusCode(404);

        // cancel-by-no-spk
        doNothing().when(tbPenjualanService).cancelPenjualanBySpk("SPK001");
        given().when().delete("/api/pazaauto/penjualan/cancel-by-no-spk/SPK001").then().statusCode(200);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  TbSpkResource — CRUD + custom endpoints
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("TbSpk full")
    void spkFull() {
        TbSpkEntity s = new TbSpkEntity();
        s.setId(1L);
        s.setNoSpk("SPK20250101001");

        // list
        when(tbSpkService.findAll()).thenReturn(List.of(s));
        given().when().get("/api/pazaauto/spk").then().statusCode(200);

        // paginated
        when(tbSpkService.findPaginated(any())).thenReturn(new PageResponse<>(List.of(s), 1, 10, 1));
        given().when().get("/api/pazaauto/spk/paginated").then().statusCode(200);

        // get by id
        when(tbSpkService.findById(1L)).thenReturn(s);
        given().when().get("/api/pazaauto/spk/1").then().statusCode(200);

        // create
        doNothing().when(tbSpkService).enrich(any());
        when(tbSpkService.create(any())).thenReturn(s);
        given().contentType(ContentType.JSON).body(Map.of("noSpk", "SPK001"))
            .when().post("/api/pazaauto/spk").then().statusCode(200);

        // update
        doNothing().when(tbSpkService).enrich(any());
        when(tbSpkService.update(anyLong(), any())).thenReturn(s);
        given().contentType(ContentType.JSON).body(Map.of("status", "PROSES"))
            .when().put("/api/pazaauto/spk/1").then().statusCode(200);

        // delete (cancel)
        when(tbSpkService.cancelSpk(1L)).thenReturn(s);
        given().when().delete("/api/pazaauto/spk/1").then().statusCode(200);

        // delete — not found
        when(tbSpkService.cancelSpk(999L)).thenReturn(null);
        given().when().delete("/api/pazaauto/spk/999").then().statusCode(200)
            .body("error", containsString("not found"));

        // by-no-spk
        when(tbSpkService.findByNoSpk("SPK001")).thenReturn(s);
        given().when().get("/api/pazaauto/spk/by-no-spk/SPK001").then().statusCode(200);

        // unprocessed
        when(tbSpkService.findUnprocessedSpk()).thenReturn(List.of(s));
        given().when().get("/api/pazaauto/spk/unprocessed").then().statusCode(200);

        // get-next-spk-number
        when(tbSpkService.generateNextSpkNumber(anyString())).thenReturn("SPK20250101002");
        given().when().get("/api/pazaauto/spk/get-next-spk-number").then().statusCode(200);

        // delete-by-no-spk
        doNothing().when(tbSpkService).deleteByNoSpk("SPK001");
        given().when().delete("/api/pazaauto/spk/delete-by-no-spk/SPK001").then().statusCode(200);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  TbSpkDetailResource — CRUD (composite key)
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("TbSpkDetail CRUD")
    void spkDetailCrud() {
        TbSpkDetailEntity d = new TbSpkDetailEntity();
        TbSpkDetailId id = new TbSpkDetailId("SPK001", "Cuci");
        d.setId(id);

        when(tbSpkDetailService.findAll()).thenReturn(List.of(d));
        given().when().get("/api/pazaauto/spk-detail").then().statusCode(200);

        when(tbSpkDetailService.findPaginated(any())).thenReturn(new PageResponse<>(List.of(d), 1, 10, 1));
        given().when().get("/api/pazaauto/spk-detail/paginated").then().statusCode(200);

        when(tbSpkDetailService.findById(any())).thenReturn(d);
        given().when().get("/api/pazaauto/spk-detail/SPK001:Cuci").then().statusCode(200);

        when(tbSpkDetailService.create(any())).thenReturn(d);
        given().contentType(ContentType.JSON).body(Map.of("id", Map.of("noSpk", "SPK001", "namaJasa", "Cuci")))
            .when().post("/api/pazaauto/spk-detail").then().statusCode(200);

        when(tbSpkDetailService.update(any(), any())).thenReturn(d);
        given().contentType(ContentType.JSON).body(Map.of("harga", 50000))
            .when().put("/api/pazaauto/spk-detail/SPK001:Cuci").then().statusCode(200);

        doNothing().when(tbSpkDetailService).delete(any());
        given().when().delete("/api/pazaauto/spk-detail/SPK001:Cuci").then().statusCode(200);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  TbRekapPenjualanResource
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("TbRekapPenjualan full")
    void rekapPenjualanFull() {
        // list
        when(tbSpkService.findAll()).thenReturn(List.of());
        given().when().get("/api/pazaauto/rekap-penjualan").then().statusCode(200);

        // by-no-spk
        when(tbSpkService.findByNoSpk("SPK001")).thenReturn(new TbSpkEntity());
        given().when().get("/api/pazaauto/rekap-penjualan/by-no-spk/SPK001").then().statusCode(200);

        // unprocessed
        when(tbSpkService.findUnprocessedSpk()).thenReturn(List.of());
        given().when().get("/api/pazaauto/rekap-penjualan/unprocessed").then().statusCode(200);

        // get by id
        when(tbSpkService.findByIdWithPenjualan(1L)).thenReturn(new RekapPenjualanDto());
        given().when().get("/api/pazaauto/rekap-penjualan/1").then().statusCode(200);

        // paginated
        when(tbSpkService.findPaginatedWithPenjualan(any())).thenReturn(new PageResponse<>(List.of(), 1, 10, 0));
        given().when().get("/api/pazaauto/rekap-penjualan/paginated").then().statusCode(200);

        // get-next-spk-number
        when(tbSpkService.generateNextSpkNumber(anyString())).thenReturn("SPK20250101001");
        given().when().get("/api/pazaauto/rekap-penjualan/get-next-spk-number").then().statusCode(200);

        // create returns 304 Not Modified
        given().contentType(ContentType.JSON).body(Map.of())
            .when().post("/api/pazaauto/rekap-penjualan")
            .then().statusCode(304);

        // update returns 304 Not Modified
        given().contentType(ContentType.JSON).body(Map.of())
            .when().put("/api/pazaauto/rekap-penjualan/1")
            .then().statusCode(304);

        // delete (cancel) found
        when(tbSpkService.cancelSpk(1L)).thenReturn(new TbSpkEntity());
        given().when().delete("/api/pazaauto/rekap-penjualan/1").then().statusCode(200);

        // delete not found
        when(tbSpkService.cancelSpk(999L)).thenReturn(null);
        given().when().delete("/api/pazaauto/rekap-penjualan/999").then().statusCode(200)
            .body("error", containsString("not found"));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  TbSparepartResource — CRUD + paginated with supplierId
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("TbSparepart full")
    void sparepartFull() {
        TbSparepartEntity s = new TbSparepartEntity();
        s.setId(1L);

        when(tbSparepartService.findAll()).thenReturn(List.of(s));
        given().when().get("/api/pazaauto/sparepart").then().statusCode(200);

        // custom paginated with supplierId
        when(tbSparepartService.findPaginated(any())).thenReturn(new PageResponse<>(List.of(s), 1, 10, 1));
        given().queryParam("supplierId", 1).when().get("/api/pazaauto/sparepart/paginated").then().statusCode(200);

        when(tbSparepartService.findById(1L)).thenReturn(s);
        given().when().get("/api/pazaauto/sparepart/1").then().statusCode(200);

        when(tbSparepartService.create(any())).thenReturn(s);
        given().contentType(ContentType.JSON).body(Map.of("namaSparepart", "filter"))
            .when().post("/api/pazaauto/sparepart").then().statusCode(200);

        when(tbSparepartService.update(anyLong(), any())).thenReturn(s);
        given().contentType(ContentType.JSON).body(Map.of("hargaJual", 50000))
            .when().put("/api/pazaauto/sparepart/1").then().statusCode(200);

        doNothing().when(tbSparepartService).delete(1L);
        given().when().delete("/api/pazaauto/sparepart/1").then().statusCode(200);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  SummaryResource — requires Owner role
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("GET /api/pazaauto/summary — requires Owner")
    @TestSecurity(user = "owner", roles = {"Owner"})
    void summarySuccess() {
        when(summaryService.getSummary(anyString(), anyString(), anyString())).thenReturn(new SummaryDto());
        given()
            .queryParam("startDate", "2025-01-01").queryParam("endDate", "2025-01-31")
            .when().get("/api/pazaauto/summary")
            .then().statusCode(200);
    }

    @Test
    @DisplayName("GET /api/pazaauto/summary — 403 without Owner role")
    @TestSecurity(user = "user", roles = {"user"})
    void summaryForbidden() {
        given()
            .when().get("/api/pazaauto/summary")
            .then().statusCode(403);
    }

    @Test
    @DisplayName("GET /api/pazaauto/summary — defaults dates when omitted")
    @TestSecurity(user = "owner", roles = {"Owner"})
    void summaryDefaultDates() {
        when(summaryService.getSummary(anyString(), anyString(), isNull())).thenReturn(new SummaryDto());
        given()
            .when().get("/api/pazaauto/summary")
            .then().statusCode(200);
    }
}
