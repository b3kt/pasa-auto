package com.github.b3kt.presentation.rest.pazaauto;

import com.github.b3kt.application.service.pazaauto.AbstractCrudService;
import com.github.b3kt.application.service.pazaauto.TbSupplierService;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSupplierEntity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;

@RequestScoped
@Path("/api/pazaauto/supplier")
public class TbSupplierResource extends AbstractCrudResource<TbSupplierEntity, Integer> {

    @Inject
    TbSupplierService service;

    @Override
    protected AbstractCrudService<TbSupplierEntity, Integer> getService() {
        return service;
    }

    @Override
    protected Integer parseId(String id) {
        return Integer.valueOf(id);
    }

    @Override
    protected String getEntityName() {
        return "Supplier";
    }
    @jakarta.ws.rs.core.Context
    jakarta.ws.rs.core.UriInfo uriInfo;

    @Override
    public jakarta.ws.rs.core.Response list() {
        String search = uriInfo.getQueryParameters().getFirst("search");
        if (search != null && !search.isEmpty()) {
            return jakarta.ws.rs.core.Response.ok(com.github.b3kt.application.dto.ApiResponse.success(service.search(search))).build();
        }
        return super.list();
    }
}
