package com.github.b3kt.presentation.rest.pazaauto;

import com.github.b3kt.application.dto.ApiResponse;
import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.application.dto.PembelianWithDetailsRequest;
import com.github.b3kt.application.dto.pazaauto.PembelianDto;
import com.github.b3kt.application.mapper.pazaauto.PembelianMapper;
import com.github.b3kt.application.service.pazaauto.TbPembelianDetailService;
import com.github.b3kt.application.service.pazaauto.TbPembelianService;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPembelianEntity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Optional;

@RequestScoped
@Path("/api/pazaauto/pembelian")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TbPembelianResource {

    @Inject
    TbPembelianService service;

    @Inject
    TbPembelianDetailService tbPembelianDetailService;

    @Inject
    PembelianMapper pembelianMapper;

    @GET
    public Response findAll() {
        return Response.ok(ApiResponse.success(pembelianMapper.toDtoList(service.findAll()))).build();
    }

    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") String id) {
        TbPembelianEntity entity;
        try {
            entity = service.findById(Long.valueOf(id));
        } catch (NumberFormatException e) {
            entity = service.findByNoPembelian(id);
            if (entity != null) {
                Optional.ofNullable(tbPembelianDetailService.findByPembelianId(entity.getId()))
                        .ifPresent(entity::setDetails);
            }
        }
        if (entity == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ApiResponse.error("Pembelian not found"))
                    .build();
        }
        return Response.ok(ApiResponse.success(pembelianMapper.toDto(entity))).build();
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
            @QueryParam("jenisPembelian") String jenisPembelian,
            @QueryParam("kategoriOperasional") String kategoriOperasional,
            @QueryParam("startDate") String startDate,
            @QueryParam("endDate") String endDate) {

        PageRequest pageRequest = new PageRequest(page, rowsPerPage);
        pageRequest.setSortBy(sortBy);
        pageRequest.setDescending(descending);
        pageRequest.setSearch(search);
        pageRequest.setStatusFilter(statusFilter);
        pageRequest.setFilterToday(filterToday);
        pageRequest.setJenisPembelianFilter(jenisPembelian);
        pageRequest.setKategoriOperasionalFilter(kategoriOperasional);
        pageRequest.setStartDate(startDate);
        pageRequest.setEndDate(endDate);

        PageResponse<TbPembelianEntity> pageResponse = service.findPaginated(pageRequest);
        return Response.ok(ApiResponse.success(
                new PageResponse<>(
                        pembelianMapper.toDtoList(pageResponse.getRows()),
                        pageResponse.getCurrentPage(),
                        pageResponse.getRowsPerPage(),
                        pageResponse.getTotalRows()))).build();
    }

    @GET
    @Path("/by-no/{noPembelian}")
    public Response getByNoPembelian(@PathParam("noPembelian") String noPembelian) {
        try {
            TbPembelianEntity entity = service.findByNoPembelian(noPembelian);
            if (entity == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(ApiResponse.error("Pembelian not found"))
                        .build();
            }

            Optional.ofNullable(tbPembelianDetailService.findByPembelianId(entity.getId()))
                            .ifPresent(entity::setDetails);

            return Response.ok(ApiResponse.success(pembelianMapper.toDto(entity))).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Failed to fetch pembelian: " + e.getMessage()))
                    .build();
        }
    }

    @POST
    public Response create(PembelianDto dto) {
        TbPembelianEntity entity = pembelianMapper.toEntity(dto);
        TbPembelianEntity created = service.create(entity);
        return Response.ok(ApiResponse.success("Pembelian created", pembelianMapper.toDto(created))).build();
    }

    @POST
    @Path("/with-details")
    public Response createWithDetails(PembelianWithDetailsRequest request) {
        TbPembelianEntity created = service.createWithDetails(request.getPembelian(), request.getDetails());
        return Response.ok(ApiResponse.success("Pembelian created with details", pembelianMapper.toDto(created))).build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") String id, PembelianDto dto) {
        TbPembelianEntity entity = pembelianMapper.toEntity(dto);
        TbPembelianEntity updated = service.update(Long.valueOf(id), entity);
        if (updated == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ApiResponse.error("Pembelian not found"))
                    .build();
        }
        return Response.ok(ApiResponse.success("Pembelian updated", pembelianMapper.toDto(updated))).build();
    }

    @PUT
    @Path("/{id}/with-details")
    public Response updateWithDetails(@PathParam("id") String id,
            PembelianWithDetailsRequest request) {
        TbPembelianEntity updated = service.updateWithDetails(Long.valueOf(id), request.getPembelian(),
                request.getDetails());
        return Response.ok(ApiResponse.success("Pembelian updated with details", pembelianMapper.toDto(updated))).build();
    }

    @GET
    @Path("/get-next-number")
    public Response getNextPembelianNumber(@QueryParam("jenisPembelian") @DefaultValue("SPAREPART") String jenisPembelian) {
        try {
            String noPembelian = service.generateNoPembelian(jenisPembelian);
            return Response.ok(ApiResponse.success(noPembelian)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Failed to generate no pembelian: " + e.getMessage())).build();
        }
    }

    @GET
    @Path("/generate-no")
    public Response generateNoPembelian(@QueryParam("jenisPembelian") String jenisPembelian) {
         try {
             String noPembelian = service.generateNoPembelian(jenisPembelian);
             return Response.ok(ApiResponse.success(noPembelian)).build();
         } catch (Exception e) {
             return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                     .entity(ApiResponse.error("Failed to generate no pembelian: " + e.getMessage())).build();
         }
    }

    @jakarta.ws.rs.DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") String id) {
        service.delete(Long.valueOf(id));
        return Response.ok(ApiResponse.success("Pembelian deleted")).build();
    }
}
