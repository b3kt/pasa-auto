package com.github.b3kt.presentation.rest.pazaauto;

import com.github.b3kt.application.dto.ApiResponse;
import com.github.b3kt.application.dto.pazaauto.VehicleHistoryDto;
import com.github.b3kt.application.dto.pazaauto.VehicleTransactionDto;
import com.github.b3kt.application.service.pazaauto.KendaraanOwnershipService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

import java.util.List;

@RequestScoped
@Path("/api/pazaauto/vehicles")
public class PelangganVehicleResource {

    @Inject
    KendaraanOwnershipService ownershipService;

    @GET
    @Path("/by-nopol/{nopol}")
    public Response getByNopol(@PathParam("nopol") String nopol) {
        VehicleHistoryDto dto = ownershipService.getVehicleHistory(nopol);
        if (dto == null) {
            return Response.ok(ApiResponse.error("Vehicle not found for nopol: " + nopol)).build();
        }
        return Response.ok(ApiResponse.success(dto)).build();
    }

    @GET
    @Path("/{nopol}/transactions")
    public Response getTransactions(@PathParam("nopol") String nopol) {
        List<VehicleTransactionDto> transactions = ownershipService.findTransactionsByNopol(nopol);
        return Response.ok(ApiResponse.success(transactions)).build();
    }
}