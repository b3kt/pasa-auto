package com.github.b3kt.presentation.rest.pazaauto;

import com.github.b3kt.application.service.pazaauto.AbstractCrudService;
import com.github.b3kt.application.service.pazaauto.TbBarangService;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbBarangEntity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;

@RequestScoped
@Path("/api/pazaauto/barang")
public class TbBarangResource extends AbstractCrudResource<TbBarangEntity, Long> {

    @Inject
    TbBarangService service;

    @Override
    protected AbstractCrudService<TbBarangEntity, Long> getService() {
        return service;
    }

    @Override
    protected Long parseId(String id) {
        return Long.valueOf(id);
    }

    @Override
    protected String getEntityName() {
        return "Barang";
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
