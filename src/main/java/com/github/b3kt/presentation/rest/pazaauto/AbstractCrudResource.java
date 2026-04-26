package com.github.b3kt.presentation.rest.pazaauto;

import com.github.b3kt.application.dto.ApiResponse;
import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.application.service.pazaauto.AbstractCrudService;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public abstract class AbstractCrudResource<T, ID> {

    protected abstract AbstractCrudService<T, ID> getService();

    protected abstract ID parseId(String id);

    protected abstract String getEntityName();

    protected static final DateTimeFormatter SPK_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @GET
    @WithSpan("list-all-entities")
    public Response list() {
        List<T> items = getService().findAll();
        return Response.ok(ApiResponse.success(items)).build();
    }

    @GET
    @Path("/paginated")
    @WithSpan("list-paginated-entities")
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

        PageResponse<T> pageResponse = getService().findPaginated(pageRequest);
        return Response.ok(ApiResponse.success(pageResponse)).build();
    }

    @GET
    @Path("/{id}")
    @WithSpan("get-entity-by-id")
    public Response getById(@PathParam("id") @SpanAttribute("entity.id") String id) {
        T entity = getService().findById(parseId(id));
        return Response.ok(ApiResponse.success(entity)).build();
    }

    @POST
    @WithSpan("create-entity")
    public Response create(T entity) {
        T created = getService().create(entity);
        return Response.ok(ApiResponse.success(getEntityName() + " created", created)).build();
    }

    @PUT
    @Path("/{id}")
    @WithSpan("update-entity")
    public Response update(@PathParam("id") @SpanAttribute("entity.id") String id, T entity) {
        T updated = getService().update(parseId(id), entity);
        return Response.ok(ApiResponse.success(getEntityName() + " updated", updated)).build();
    }

    @DELETE
    @Path("/{id}")
    @WithSpan("delete-entity")
    public Response delete(@PathParam("id") @SpanAttribute("entity.id") String id) {
        getService().delete(parseId(id));
        return Response.ok(ApiResponse.success(getEntityName() + " deleted", null)).build();
    }
}
