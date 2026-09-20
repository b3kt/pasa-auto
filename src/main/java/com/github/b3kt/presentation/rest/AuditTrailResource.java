package com.github.b3kt.presentation.rest;

import com.github.b3kt.infrastructure.security.Roles;
import com.github.b3kt.application.dto.ApiResponse;
import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.application.service.AuditTrailService;
import com.github.b3kt.infrastructure.persistence.entity.AuditTrailEntity;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
@Path("/api/audit-trail")
@RolesAllowed({Roles.ADMIN, Roles.OWNER})
public class AuditTrailResource {

    /**
     * tb_audit_trail only ever grows - V10 blocks deletes and 51 triggers write to it - so every
     * list below returns the newest rows up to a limit rather than the whole table.
     */
    private static final int DEFAULT_LIMIT = 200;
    private static final int MAX_LIMIT = 1000;

    private static int cappedLimit(int limit) {
        return limit < 1 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
    }

    @Inject
    AuditTrailService auditTrailService;

    @GET
    @WithSpan("list-audit-trail")
    public Response list(@QueryParam("limit") @DefaultValue("200") int limit) {
        List<AuditTrailEntity> items = auditTrailService.findRecent(cappedLimit(limit));
        return Response.ok(ApiResponse.success(items)).build();
    }

    /** Distinct usernames for the audit page filter, so it no longer reads the table to build one. */
    @GET
    @Path("/usernames")
    @WithSpan("list-audit-trail-usernames")
    public Response usernames() {
        return Response.ok(ApiResponse.success(auditTrailService.findDistinctUsernames())).build();
    }

    @GET
    @Path("/paginated")
    @WithSpan("list-audit-trail-paginated")
    public Response listPaginated(
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("rowsPerPage") @DefaultValue("10") int rowsPerPage,
            @QueryParam("sortBy") String sortBy,
            @QueryParam("descending") @DefaultValue("true") boolean descending,
            @QueryParam("search") String search) {

        PageRequest pageRequest = new PageRequest(page, rowsPerPage);
        pageRequest.setSortBy(sortBy);
        pageRequest.setDescending(descending);
        pageRequest.setSearch(search);

        PageResponse<AuditTrailEntity> pageResponse = auditTrailService.findPaginated(pageRequest);
        return Response.ok(ApiResponse.success(pageResponse)).build();
    }

    @GET
    @Path("/table/{tableName}")
    @WithSpan("get-audit-trail-by-table")
    public Response getByTable(@PathParam("tableName") String tableName,
            @QueryParam("limit") @DefaultValue("200") int limit) {
        List<AuditTrailEntity> items = auditTrailService.findByTableName(tableName, cappedLimit(limit));
        return Response.ok(ApiResponse.success(items)).build();
    }

    @GET
    @Path("/record/{recordId}")
    @WithSpan("get-audit-trail-by-record")
    public Response getByRecord(@PathParam("recordId") Long recordId,
            @QueryParam("limit") @DefaultValue("200") int limit) {
        List<AuditTrailEntity> items = auditTrailService.findByRecordId(recordId, cappedLimit(limit));
        return Response.ok(ApiResponse.success(items)).build();
    }

    @GET
    @Path("/table/{tableName}/record/{recordId}")
    @WithSpan("get-audit-trail-by-table-and-record")
    public Response getByTableAndRecord(
            @PathParam("tableName") String tableName,
            @PathParam("recordId") Long recordId,
            @QueryParam("limit") @DefaultValue("200") int limit) {
        List<AuditTrailEntity> items = auditTrailService.findByTableNameAndRecordId(tableName, recordId, cappedLimit(limit));
        return Response.ok(ApiResponse.success(items)).build();
    }

    @GET
    @Path("/user/{userId}")
    @WithSpan("get-audit-trail-by-user")
    public Response getByUser(@PathParam("userId") Long userId,
            @QueryParam("limit") @DefaultValue("200") int limit) {
        List<AuditTrailEntity> items = auditTrailService.findByUserId(userId, cappedLimit(limit));
        return Response.ok(ApiResponse.success(items)).build();
    }
}