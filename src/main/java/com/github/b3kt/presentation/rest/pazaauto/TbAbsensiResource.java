package com.github.b3kt.presentation.rest.pazaauto;

import com.github.b3kt.infrastructure.security.Roles;
import jakarta.annotation.security.RolesAllowed;
import com.github.b3kt.application.dto.ApiResponse;
import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.application.dto.pazaauto.AbsensiDto;
import com.github.b3kt.application.mapper.pazaauto.AbsensiMapper;
import com.github.b3kt.application.service.pazaauto.TbAbsensiService;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbAbsensiEntity;
import io.quarkus.security.identity.SecurityIdentity;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.SocketAddress;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.time.LocalDate;
import java.util.Map;

@lombok.extern.slf4j.Slf4j
@RequestScoped
@Path("/api/pazaauto/absensi")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
/*
 * Employees (Karyawan) may only clock in/out and read their own attendance; the karyawanId comes from
 * their token. Listing, editing and marking absences is limited to Admin/Owner.
 */
@RolesAllowed({Roles.ADMIN, Roles.OWNER, Roles.KARYAWAN})
public class TbAbsensiResource {

    @Inject
    TbAbsensiService service;

    @Inject
    AbsensiMapper absensiMapper;

    @Inject
    SecurityIdentity identity;

    @GET
    @RolesAllowed({Roles.ADMIN, Roles.OWNER})
    public Response findAll() {
        return Response.ok(ApiResponse.success(absensiMapper.toDtoList(service.findAll()))).build();
    }

    @POST
    @RolesAllowed({Roles.ADMIN, Roles.OWNER})
    public Response create(AbsensiDto dto) {
        TbAbsensiEntity entity = absensiMapper.toEntity(dto);
        TbAbsensiEntity created = service.create(entity);
        return Response.ok(ApiResponse.success(absensiMapper.toDto(created))).build();
    }

    @POST
    @Path("/clock-in")
    public Response clockIn(
            AbsensiDto dto,
            @Context HttpServerRequest request) {
        TbAbsensiEntity entity = absensiMapper.toEntity(dto);
        Long karyawanId = resolveKaryawanId(entity.getKaryawanId());
        try {
            String ipAddress = getClientIpAddress(request);
            TbAbsensiEntity result = service.clockIn(karyawanId, ipAddress, entity.getDeviceInfo(), entity.getLokasiMasuk());
            return Response.ok(ApiResponse.success("Clock-in successful", absensiMapper.toDto(result))).build();
        } catch (IllegalStateException | SecurityException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error(e.getMessage()))
                    .build();
        } catch (Exception e) {
            log.error("Failed to clock in", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Failed to clock in"))
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
            @Context HttpServerRequest request) {
        TbAbsensiEntity entity = absensiMapper.toEntity(dto);
        Long karyawanId = resolveKaryawanId(entity.getKaryawanId());
        try {
            String ipAddress = getClientIpAddress(request);
            TbAbsensiEntity result = service.clockOut(karyawanId, ipAddress, entity.getLokasiKeluar(), entity.getKeterangan());
            return Response.ok(ApiResponse.success("Clock-out successful", absensiMapper.toDto(result))).build();
        } catch (IllegalStateException | SecurityException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error(e.getMessage()))
                    .build();
        } catch (Exception e) {
            log.error("Failed to clock out", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Failed to clock out"))
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
        resolveKaryawanId(entity.getKaryawanId());
        return Response.ok(ApiResponse.success(absensiMapper.toDto(entity))).build();
    }

    @GET
    @Path("/paginated")
    @RolesAllowed({Roles.ADMIN, Roles.OWNER})
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
        Long ownKaryawanId = resolveKaryawanId(karyawanId);
        try {
            TbAbsensiEntity result = service.getTodayAttendance(ownKaryawanId);
            if (result == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(ApiResponse.error("No attendance found for today"))
                        .build();
            }
            return Response.ok(ApiResponse.success("Today's attendance retrieved", absensiMapper.toDto(result))).build();
        } catch (Exception e) {
            log.error("Failed to get today's attendance", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Failed to get today's attendance"))
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
        // Admin/Owner may omit karyawanId to see everyone; employees always get their own history
        Long historyKaryawanId = resolveKaryawanId(karyawanId);
        try {
            PageRequest pageRequest = new PageRequest(page, rowsPerPage);
            pageRequest.setSortBy(sortBy);
            pageRequest.setDescending(descending);

            LocalDate start = startDate != null ? LocalDate.parse(startDate) : null;
            LocalDate end = endDate != null ? LocalDate.parse(endDate) : null;

            PageResponse<TbAbsensiEntity> result = service.getAttendanceHistory(
                    historyKaryawanId, start, end, status, pageRequest);
            return Response.ok(ApiResponse.success("Attendance history retrieved",
                    new PageResponse<>(
                            absensiMapper.toDtoList(result.getRows()),
                            result.getCurrentPage(),
                            result.getRowsPerPage(),
                            result.getTotalRows()))).build();
        } catch (Exception e) {
            log.error("Failed to get attendance history", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Failed to get attendance history"))
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
        Long ownKaryawanId = resolveKaryawanId(karyawanId);
        try {
            Map<String, Object> summary = service.getMonthlySummary(ownKaryawanId, month, year);
            return Response.ok(ApiResponse.success("Monthly summary retrieved", summary)).build();
        } catch (Exception e) {
            log.error("Failed to get monthly summary", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Failed to get monthly summary"))
                    .build();
        }
    }

    /**
     * Admin marks absence
     */
    @POST
    @Path("/mark-absence")
    @RolesAllowed({Roles.ADMIN, Roles.OWNER})
    public Response markAbsence(AbsensiDto dto) {
        try {
            TbAbsensiEntity entity = absensiMapper.toEntity(dto);
            TbAbsensiEntity result = service.markAbsence(entity.getKaryawanId(), entity.getTanggal(), entity.getStatus(), entity.getKeterangan());
            return Response.ok(ApiResponse.success("Absence marked successfully", absensiMapper.toDto(result))).build();
        } catch (Exception e) {
            log.error("Failed to mark absence", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Failed to mark absence"))
                    .build();
        }
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({Roles.ADMIN, Roles.OWNER})
    public Response update(@PathParam("id") String id, AbsensiDto dto) {
        TbAbsensiEntity entity = absensiMapper.toEntity(dto);
        TbAbsensiEntity updated = service.update(Long.valueOf(id), entity);
        return Response.ok(ApiResponse.success(absensiMapper.toDto(updated))).build();
    }

    @jakarta.ws.rs.DELETE
    @Path("/{id}")
    @RolesAllowed({Roles.ADMIN, Roles.OWNER})
    public Response delete(@PathParam("id") String id) {
        service.delete(Long.valueOf(id));
        return Response.ok(ApiResponse.success("Absensi deleted")).build();
    }

    /**
     * Get client IP address from headers
     */
    /**
     * Admin/Owner may act on any employee, so the requested id is used as-is.
     * Employees may only act on themselves: returns their own karyawanId from the token,
     * or 403 when the request targets someone else or their account isn't linked to an employee.
     */
    private Long resolveKaryawanId(Long requestedKaryawanId) {
        if (identity.hasRole(Roles.ADMIN) || identity.hasRole(Roles.OWNER)) {
            return requestedKaryawanId;
        }
        Long ownKaryawanId = null;
        if (identity.getPrincipal() instanceof JsonWebToken jwt && jwt.getClaim("karyawanId") != null) {
            ownKaryawanId = Long.valueOf(jwt.getClaim("karyawanId").toString());
        }
        if (ownKaryawanId == null || (requestedKaryawanId != null && !ownKaryawanId.equals(requestedKaryawanId))) {
            throw new ForbiddenException("You can only access your own attendance");
        }
        return ownKaryawanId;
    }

    /**
     * The address of the connecting client. Client-supplied headers such as X-Forwarded-For are not read
     * here: they could be forged to pass the clock-in IP allowlist. Behind a reverse proxy, enable
     * {@code quarkus.http.proxy.proxy-address-forwarding} with {@code trusted-proxies} so Quarkus resolves
     * the client address from the proxy's headers.
     */
    private String getClientIpAddress(HttpServerRequest request) {
        SocketAddress remoteAddress = request.remoteAddress();
        if (remoteAddress != null) {
           return remoteAddress.hostAddress();
        }
        return null ;
    }
}
