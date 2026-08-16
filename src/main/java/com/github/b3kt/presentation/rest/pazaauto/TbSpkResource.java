package com.github.b3kt.presentation.rest.pazaauto;

import com.github.b3kt.application.dto.ApiResponse;
import com.github.b3kt.application.dto.pazaauto.SpkDto;
import com.github.b3kt.application.mapper.pazaauto.SpkMapper;
import com.github.b3kt.application.service.pazaauto.TbSpkService;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSpkEntity;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.mutiny.Uni;
import io.quarkus.rate-limiting.RateLimit;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

@RequestScoped
@Path("/api/pazaauto/spk")
public class TbSpkResource {

    @Inject
    TbSpkService service;

    @Inject
    SpkMapper spkMapper;

    @GET
    @Path("/by-no-spk/{noSpk}")
    @WithSpan("find-spk-by-no-spk")
    @RateLimit(value = 30, window = 60, unit = java.util.concurrent.TimeUnit.SECONDS)
    public Response findByNoSpk(@PathParam("noSpk") @SpanAttribute("spk.no-spk") String noSpk) {
        TbSpkEntity entity = service.findByNoSpk(noSpk);
        if (entity == null) {
            return Response.ok(ApiResponse.error("SPK not found")).build();
        }
        return Response.ok(ApiResponse.success(spkMapper.toDto(entity))).build();
    }

    @GET
    @Path("/unprocessed")
    @WithSpan("get-unprocessed-spk")
    @RateLimit(value = 30, window = 60, unit = java.util.concurrent.TimeUnit.SECONDS)
    public Response getUnprocessedSpk() {
        return Response.ok(ApiResponse.success(spkMapper.toDtoList(service.findUnprocessedSpk()))).build();
    }

    @GET
    @Path("/{id}")
    @WithSpan("get-spk-by-id")
    @RateLimit(value = 30, window = 60, unit = java.util.concurrent.TimeUnit.SECONDS)
    public Response getById(@PathParam("id") String id) {
        TbSpkEntity entity = service.findById(Long.valueOf(id));
        if (entity == null) {
            return Response.ok(ApiResponse.error("SPK not found")).build();
        }
        return Response.ok(ApiResponse.success(spkMapper.toDto(entity))).build();
    }

    @GET
    @Path("/paginated")
    @WithSpan("list-paginated-spk")
    @RateLimit(value = 20, window = 60, unit = java.util.concurrent.TimeUnit.SECONDS)
    public Response listPaginated(
            @jakarta.ws.rs.QueryParam("page") @jakarta.ws.rs.DefaultValue("1") int page,
            @jakarta.ws.rs.QueryParam("rowsPerPage") @jakarta.ws.rs.DefaultValue("10") int rowsPerPage,
            @jakarta.ws.rs.QueryParam("sortBy") String sortBy,
            @jakarta.ws.rs.QueryParam("descending") @jakarta.ws.rs.DefaultValue("false") boolean descending,
            @jakarta.ws.rs.QueryParam("search") String search,
            @jakarta.ws.rs.QueryParam("statusFilter") String statusFilter,
            @jakarta.ws.rs.QueryParam("filterToday") @jakarta.ws.rs.DefaultValue("false") boolean filterToday,
            @jakarta.ws.rs.QueryParam("startDate") String startDate,
            @jakarta.ws.rs.QueryParam("endDate") String endDate) {

        com.github.b3kt.application.dto.PageRequest pageRequest = new com.github.b3kt.application.dto.PageRequest(page, rowsPerPage);
        pageRequest.setSortBy(sortBy);
        pageRequest.setDescending(descending);
        pageRequest.setSearch(search);
        pageRequest.setStatusFilter(statusFilter);
        pageRequest.setFilterToday(filterToday);
        pageRequest.setStartDate(startDate);
        pageRequest.setEndDate(endDate);

        com.github.b3kt.application.dto.PageResponse<TbSpkEntity> pageResponse = service.findPaginated(pageRequest);
        return Response.ok(ApiResponse.success(
                new com.github.b3kt.application.dto.PageResponse<>(
                        spkMapper.toDtoList(pageResponse.getRows()),
                        pageResponse.getCurrentPage(),
                        pageResponse.getRowsPerPage(),
                        pageResponse.getTotalRows()))).build();
    }

    @GET
    @Path("/get-next-spk-number")
    @WithSpan("get-next-spk-number")
    @RateLimit(value = 10, window = 60, unit = java.util.concurrent.TimeUnit.SECONDS)
    public Response getNextSpk() {
        String nextSpkNumber = service.generateNextSpkNumber(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd").format(java.time.LocalDateTime.now()));
        return Response.ok(ApiResponse.success(nextSpkNumber)).build();
    }

    @POST
    @WithSpan("create-spk")
    @RateLimit(value = 10, window = 60, unit = java.util.concurrent.TimeUnit.SECONDS)
    public Response create(SpkDto dto) {
        TbSpkEntity entity = spkMapper.toEntity(dto);
        service.enrich(entity);
        TbSpkEntity created = service.create(entity);
        return Response.ok(ApiResponse.success("SPK created", spkMapper.toDto(created))).build();
    }

    @PUT
    @Path("/{id}")
    @WithSpan("update-spk")
    @RateLimit(value = 10, window = 60, unit = java.util.concurrent.TimeUnit.SECONDS)
    public Response update(@PathParam("id") String id, SpkDto dto) {
        TbSpkEntity entity = spkMapper.toEntity(dto);
        service.enrich(entity);
        TbSpkEntity updated = service.update(Long.valueOf(id), entity);

        if (updated == null) {
            return Response.ok(ApiResponse.error("SPK not found")).build();
        }
        return Response.ok(ApiResponse.success("SPK updated", spkMapper.toDto(updated))).build();
    }

    @DELETE
    @Path("/{id}")
    @WithSpan("delete-spk")
    @RateLimit(value = 10, window = 60, unit = java.util.concurrent.TimeUnit.SECONDS)
    public Response delete(@PathParam("id") String id) {
        TbSpkEntity cancelled = service.cancelSpk(Long.valueOf(id));
        if (cancelled == null) {
            return Response.ok(ApiResponse.error("SPK not found")).build();
        }
        return Response.ok(ApiResponse.success("SPK cancelled", spkMapper.toDto(cancelled))).build();
    }

    @DELETE
    @Path("/delete-by-no-spk/{noSpk}")
    @WithSpan("delete-spk-by-no-spk")
    @RateLimit(value = 5, window = 60, unit = java.util.concurrent.TimeUnit.SECONDS)
    public Response deleteByNoSpk(@PathParam("noSpk") String noSpk) {
        service.deleteByNoSpk(noSpk);
        return Response.ok(ApiResponse.success("SPK deleted permanently")).build();
    }
}
