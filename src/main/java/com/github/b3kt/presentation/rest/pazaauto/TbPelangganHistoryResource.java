package com.github.b3kt.presentation.rest.pazaauto;

import com.github.b3kt.application.dto.ApiResponse;
import com.github.b3kt.application.dto.pazaauto.KendaraanAttachRequest;
import com.github.b3kt.application.dto.pazaauto.KendaraanOwnershipDto;
import com.github.b3kt.application.dto.pazaauto.PelangganHistoryDto;
import com.github.b3kt.application.service.pazaauto.KendaraanOwnershipService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

@RequestScoped
@Path("/api/pazaauto/pelanggan")
public class TbPelangganHistoryResource {

    @Inject
    KendaraanOwnershipService ownershipService;

    @GET
    @Path("/{id}/vehicles")
    public Response getVehicles(@PathParam("id") String id) {
        return Response.ok(ApiResponse.success(
                ownershipService.listVehiclesByPelanggan(Long.valueOf(id)))).build();
    }

    @POST
    @Path("/{id}/kendaraan")
    public Response attachKendaraan(@PathParam("id") String id, @Valid KendaraanAttachRequest request) {
        try {
            KendaraanOwnershipDto dto = ownershipService.attach(
                    Long.valueOf(id),
                    request.getNopol(),
                    request.getMerk(),
                    request.getJenis(),
                    request.getTanggalMulai(),
                    request.getKeterangan());
            return Response.ok(ApiResponse.success("Kendaraan ditambahkan", dto)).build();
        } catch (IllegalArgumentException e) {
            return Response.ok(ApiResponse.error(e.getMessage())).build();
        }
    }

    @GET
    @Path("/{id}/history")
    public Response getHistory(@PathParam("id") String id) {
        PelangganHistoryDto dto = ownershipService.getPelangganHistory(Long.valueOf(id));
        if (dto == null) {
            return Response.ok(ApiResponse.error("Pelanggan not found")).build();
        }
        return Response.ok(ApiResponse.success(dto)).build();
    }
}