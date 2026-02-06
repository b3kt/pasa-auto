package com.github.b3kt.presentation.rest.pazaauto;

import com.github.b3kt.application.service.pazaauto.AbstractCrudService;
import com.github.b3kt.application.service.pazaauto.TbKaryawanPosisiService;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbKaryawanPosisiEntity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;

@RequestScoped
@Path("/api/pazaauto/karyawan-posisi")
public class TbKaryawanPosisiResource extends AbstractCrudResource<TbKaryawanPosisiEntity, Long> {

    @Inject
    TbKaryawanPosisiService service;

    @Override
    protected AbstractCrudService<TbKaryawanPosisiEntity, Long> getService() {
        return service;
    }

    @Override
    protected Long parseId(String id) {
        return Long.valueOf(id);
    }

    @Override
    protected String getEntityName() {
        return "Karyawan Posisi";
    }
}

