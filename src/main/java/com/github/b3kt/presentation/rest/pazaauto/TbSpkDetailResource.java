package com.github.b3kt.presentation.rest.pazaauto;

import com.github.b3kt.application.service.pazaauto.AbstractCrudService;
import com.github.b3kt.application.service.pazaauto.TbSpkDetailService;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSpkDetailEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSpkDetailId;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Path;

@RequestScoped
@Path("/api/pazaauto/spk-detail")
public class TbSpkDetailResource extends AbstractCrudResource<TbSpkDetailEntity, TbSpkDetailId> {

    @Inject
    TbSpkDetailService service;

    @Override
    protected AbstractCrudService<TbSpkDetailEntity, TbSpkDetailId> getService() {
        return service;
    }

    @Override
    protected TbSpkDetailId parseId(String id) {
        String[] parts = id.split(":", 2);
        if (parts.length != 2) {
            throw new BadRequestException("Invalid identifier format. Use 'noSpk:namaJasa'.");
        }
        return new TbSpkDetailId(parts[0], parts[1]);
    }

    @Override
    protected String getEntityName() {
        return "SPK Detail";
    }
}

