package com.github.b3kt.presentation.rest.pazaauto;

import com.github.b3kt.application.dto.ApiResponse;
import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.application.dto.pazaauto.AbsensiDto;
import com.github.b3kt.application.mapper.pazaauto.AbsensiMapper;
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
public class TbAbsensiResource {

    @Inject
    TbAbsensiService service;

    @Inject
    AbsensiMapper absensiMapper;

    /**
     * Clock in endpoint
     */
    @POST
    @Path("/clock-in")
    public Response clockIn(
            AbsensiDto dto,
            @HeaderParam("X-Forwarded-For") String xForwardedFor,
            @HeaderParam("X-Real-IP") String xRealIp) {
        try {
            String ipAddress = getClientIpAddress(xForwardedFor, xRealIp);
            TbAbsensiEntity entity = absensiMapper.toEntity(dto);
            TbAbsensiEntity result = service.clockIn(entity.getKaryawanId(), ipAddress, entity.getDeviceInfo(), entity.getLokasiMasuk());
            return Response.ok(ApiResponse.success("Clock-in successful", absensiMapper.toDto(result))).build();
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
            AbsensiDto dto,
            @HeaderParam("X-Forwarded-For") String xForwardedFor,
            @HeaderParam("X-Real-IP") String xRealIp) {
        try {
            String ipAddress = getClientIpAddress(xForwardedFor, xRealIp);
            TbAbsensiEntity entity = absensiMapper.toEntity(dto);
            TbAbsensiEntity result = service.clockOut(entity.getKaryawanId(), ipAddress, entity.getLokasiKeluar());
            return Response.ok(ApiResponse.success("Clock-out successful", absensiMapper.toDto(result))).build();
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

    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") String id) {
        TbAbsensiEntity entity = service.findById(Long.valueOf(id));
        if (entity == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ApiResponse.error("Absensi not found"))
                    .build();
        }
        return Response.ok(ApiResponse.success(absensiMapper.toDto(entity))).build();
    }

    @GET
    @Path("/paginated")
    public Response listPaginated(
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("rowsPerPage") @DefaultValue("10") int rowsPerPage,
            @QueryParam("sortBy") String sortBy,
            @QueryParam("descending") @DefaultValue("false") boolean descending,
            @QueryParam("search") String search,
            @QueryParam("statusFilter") String statusFilter,
            @QueryParam("filterToday") @DefaultValue("false") boolean filterToday,
            @QueryParam("startDate") String startDate,
            @QueryParam("endDate") String endDate) {

        PageRequest pageRequest = new PageRequest(page, rowsPerPage);
        pageRequest.setSortBy(sortBy);
        pageRequest.setDescending(descending);
        pageRequest.setSearch(search);
        pageRequest.setStatusFilter(statusFilter);
        pageRequest.setFilterToday(filterToday);
        pageRequest.setStartDate(startDate);
        pageRequest.setEndDate(endDate);

        PageResponse<TbAbsensiEntity> pageResponse = service.findPaginated(pageRequest);
        return Response.ok(ApiResponse.success(
                new PageResponse<>(
                        absensiMapper.toDtoList(pageResponse.getRows()),
                        pageResponse.getCurrentPage(),
                        pageResponse.getRowsPerPage(),
                        pageResponse.getTotalRows()))).build();
    }

    /**
     * Get today's attendance
     */
    @GET
    @Path("/today/{karyawanId}")
    public Response getTodayAttendance(@PathParam("karyawanId") Long karyawanId) {
        try {
            TbAbsensiEntity result = service.getTodayAttendance(karyawanId);
            if (result == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(ApiResponse.error("No attendance found for today"))
                        .build();
            }
            return Response.ok(ApiResponse.success("Today's attendance retrieved", absensiMapper.toDto(result))).build();
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
            PageRequest pageRequest = new PageRequest(page, rowsPerPage);
            pageRequest.setSortBy(sortBy);
            pageRequest.setDescending(descending);

            LocalDate start = startDate != null ? LocalDate.parse(startDate) : null;
            LocalDate end = endDate != null ? LocalDate.parse(endDate) : null;

            PageResponse<TbAbsensiEntity> result = service.getAttendanceHistory(
                    karyawanId, start, end, status, pageRequest);
            return Response.ok(ApiResponse.success("Attendance history retrieved",
                    new PageResponse<>(
                            absensiMapper.toDtoList(result.getRows()),
                            result.getCurrentPage(),
                            result.getRowsPerPage(),
                            result.getTotalRows()))).build();
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
    public Response markAbsence(AbsensiDto dto) {
        try {
            TbAbsensiEntity entity = absensiMapper.toEntity(dto);
            TbAbsensiEntity result = service.markAbsence(entity.getKaryawanId(), entity.getTanggal(), entity.getStatus(), entity.getKeterangan());
            return Response.ok(ApiResponse.success("Absence marked successfully", absensiMapper.toDto(result))).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Failed to mark absence: " + e.getMessage()))
                    .build();
        }
    }

    @jakarta.ws.rs.DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") String id) {
        service.delete(Long.valueOf(id));
        return Response.ok(ApiResponse.success("Absensi deleted")).build();
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
