package com.github.b3kt.presentation.rest.pazaauto;

import com.github.b3kt.application.dto.ApiResponse;
import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.application.service.pazaauto.AbstractCrudService;
import com.github.b3kt.application.service.pazaauto.TbAbsensiService;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbAbsensiEntity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.LocalDate;
import java.util.Map;

@RequestScoped
@Path("/api/pazaauto/absensi")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TbAbsensiResource extends AbstractCrudResource<TbAbsensiEntity, Long> {

    @Inject
    TbAbsensiService service;

    @Override
    protected AbstractCrudService<TbAbsensiEntity, Long> getService() {
        return service;
    }

    @Override
    protected Long parseId(String id) {
        return Long.parseLong(id);
    }

    @Override
    protected String getEntityName() {
        return "Absensi";
    }

    /**
     * Clock in endpoint
     */
    @POST
    @Path("/clock-in")
    public Response clockIn(
            Map<String, Object> payload,
            @HeaderParam("X-Forwarded-For") String xForwardedFor,
            @HeaderParam("X-Real-IP") String xRealIp) {
        try {
            Long karyawanId = Long.parseLong(payload.get("karyawanId").toString());
            String location = (String) payload.get("location");
            String deviceInfo = (String) payload.get("deviceInfo");
            String ipAddress = getClientIpAddress(xForwardedFor, xRealIp);

            TbAbsensiEntity result = service.clockIn(karyawanId, ipAddress, deviceInfo, location);
            return Response.ok(ApiResponse.success("Clock-in successful", result)).build();
        } catch (IllegalStateException | SecurityException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error(e.getMessage()))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Failed to clock in: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Clock out endpoint
     */
    @POST
    @Path("/clock-out")
    public Response clockOut(
            Map<String, Object> payload,
            @HeaderParam("X-Forwarded-For") String xForwardedFor,
            @HeaderParam("X-Real-IP") String xRealIp) {
        try {
            Long karyawanId = Long.parseLong(payload.get("karyawanId").toString());
            String location = (String) payload.get("location");
            String ipAddress = getClientIpAddress(xForwardedFor, xRealIp);

            TbAbsensiEntity result = service.clockOut(karyawanId, ipAddress, location);
            return Response.ok(ApiResponse.success("Clock-out successful", result)).build();
        } catch (IllegalStateException | SecurityException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error(e.getMessage()))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Failed to clock out: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get today's attendance
     */
    @GET
    @Path("/today/{karyawanId}")
    public Response getTodayAttendance(@PathParam("karyawanId") Long karyawanId) {
        try {
            TbAbsensiEntity result = service.getTodayAttendance(karyawanId);
            return Response.ok(ApiResponse.success("Today's attendance retrieved", result)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Failed to get today's attendance: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get attendance history with filters
     */
    @GET
    @Path("/history")
    public Response getHistory(
            @QueryParam("karyawanId") Long karyawanId,
            @QueryParam("startDate") String startDate,
            @QueryParam("endDate") String endDate,
            @QueryParam("status") String status,
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("rowsPerPage") @DefaultValue("10") int rowsPerPage,
            @QueryParam("sortBy") String sortBy,
            @QueryParam("descending") @DefaultValue("true") boolean descending) {
        try {
            PageRequest pageRequest = new PageRequest();
            pageRequest.setPage(page);
            pageRequest.setRowsPerPage(rowsPerPage);
            pageRequest.setSortBy(sortBy);
            pageRequest.setDescending(descending);

            LocalDate start = startDate != null ? LocalDate.parse(startDate) : null;
            LocalDate end = endDate != null ? LocalDate.parse(endDate) : null;

            PageResponse<TbAbsensiEntity> result = service.getAttendanceHistory(
                    karyawanId, start, end, status, pageRequest);
            return Response.ok(ApiResponse.success("Attendance history retrieved", result)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Failed to get attendance history: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get monthly summary
     */
    @GET
    @Path("/summary/{karyawanId}")
    public Response getMonthlySummary(
            @PathParam("karyawanId") Long karyawanId,
            @QueryParam("month") int month,
            @QueryParam("year") int year) {
        try {
            Map<String, Object> summary = service.getMonthlySummary(karyawanId, month, year);
            return Response.ok(ApiResponse.success("Monthly summary retrieved", summary)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Failed to get monthly summary: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Admin marks absence
     */
    @POST
    @Path("/mark-absence")
    public Response markAbsence(Map<String, Object> payload) {
        try {
            Long karyawanId = Long.parseLong(payload.get("karyawanId").toString());
            LocalDate tanggal = LocalDate.parse((String) payload.get("tanggal"));
            String status = (String) payload.get("status");
            String keterangan = (String) payload.get("keterangan");

            TbAbsensiEntity result = service.markAbsence(karyawanId, tanggal, status, keterangan);
            return Response.ok(ApiResponse.success("Absence marked successfully", result)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Failed to mark absence: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get client IP address from headers
     */
    private String getClientIpAddress(String xForwardedFor, String xRealIp) {
        // Try X-Forwarded-For first (proxy/load balancer)
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        // Try X-Real-IP
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp.trim();
        }
        // Default to localhost if no headers present
        return "127.0.0.1";
    }
}
