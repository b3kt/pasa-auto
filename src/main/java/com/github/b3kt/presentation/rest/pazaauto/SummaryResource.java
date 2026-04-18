package com.github.b3kt.presentation.rest.pazaauto;

import com.github.b3kt.application.dto.ApiResponse;
import com.github.b3kt.application.dto.pazaauto.SummaryDto;
import com.github.b3kt.application.service.pazaauto.SummaryService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RequestScoped
@Path("/api/pazaauto/summary")
@RequiredArgsConstructor
public class SummaryResource {

    final SummaryService summaryService;

    @GET
    @RolesAllowed("Owner")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSummary(
            @QueryParam("startDate") String startDate,
            @QueryParam("endDate") String endDate,
            @QueryParam("statusPembelianFilter") String statusPembelianFilter) {

        if (startDate == null || startDate.isBlank()) {
            startDate = LocalDate.now().withDayOfMonth(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
        if (endDate == null || endDate.isBlank()) {
            endDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        }

        SummaryDto summary = summaryService.getSummary(startDate, endDate, statusPembelianFilter);
        return Response.ok(ApiResponse.success(summary)).build();
    }
}
