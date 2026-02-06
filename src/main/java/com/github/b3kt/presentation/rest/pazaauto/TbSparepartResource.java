package com.github.b3kt.presentation.rest.pazaauto;

import com.github.b3kt.application.service.pazaauto.AbstractCrudService;
import com.github.b3kt.application.service.pazaauto.TbSparepartService;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSparepartEntity;
import com.github.b3kt.application.dto.ApiResponse;
import com.github.b3kt.application.dto.PageRequest;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

@RequestScoped
@Path("/api/pazaauto/sparepart")
public class TbSparepartResource extends AbstractCrudResource<TbSparepartEntity, Long> {

    @Inject
    TbSparepartService service;

    @Override
    protected AbstractCrudService<TbSparepartEntity, Long> getService() {
        return service;
    }

    @Override
    protected Long parseId(String id) {
        return Long.parseLong(id);
    }

    @Override
    protected String getEntityName() {
        return "Sparepart";
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
            @QueryParam("supplierId") Long supplierId) {

        PageRequest pageRequest = new PageRequest(page, rowsPerPage);
        pageRequest.setSortBy(sortBy);
        pageRequest.setDescending(descending);
        pageRequest.setSearch(search);
        pageRequest.setStatusFilter(statusFilter);
        pageRequest.setFilterToday(filterToday);
        pageRequest.setSupplierId(supplierId);

        com.github.b3kt.application.dto.PageResponse<TbSparepartEntity> pageResponse = getService()
                .findPaginated(pageRequest);
        return Response.ok(ApiResponse.success(pageResponse)).build();
    }
}
