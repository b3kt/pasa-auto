package com.github.b3kt.presentation.rest.pazaauto;

import com.github.b3kt.application.dto.ApiResponse;
import com.github.b3kt.application.dto.pazaauto.KendaraanOwnershipDto;
import com.github.b3kt.application.dto.pazaauto.KendaraanTransferRequest;
import com.github.b3kt.application.service.pazaauto.KendaraanOwnershipService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

import java.util.List;

@RequestScoped
@Path("/api/pazaauto/kendaraan")
public class TbKendaraanOwnershipResource {

    @Inject
    KendaraanOwnershipService ownershipService;

    @GET
    @Path("/{id}/owners")
    public Response getOwners(@PathParam("id") String id) {
        List<KendaraanOwnershipDto> owners = ownershipService.listOwnerHistoryByMasterId(Long.valueOf(id));
        return Response.ok(ApiResponse.success(owners)).build();
    }

    @POST
    @Path("/transfer")
    public Response transfer(@Valid KendaraanTransferRequest request) {
        try {
            KendaraanOwnershipDto dto = ownershipService.transfer(
                    request.getNopol(),
                    request.getIdPelangganBaru(),
                    request.getTanggalAwal(),
                    request.getKeterangan());
            return Response.ok(ApiResponse.success("Kendaraan ditransfer", dto)).build();
        } catch (IllegalArgumentException e) {
            return Response.ok(ApiResponse.error(e.getMessage())).build();
        }
    }
}