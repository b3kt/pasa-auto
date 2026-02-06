package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPenjualanEntity;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbPenjualanRepository;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class TbPenjualanService extends AbstractCrudService<TbPenjualanEntity, String> {

    @Inject
    TbPenjualanRepository repository;

    @Override
    protected PanacheRepositoryBase<TbPenjualanEntity, String> getRepository() {
        return repository;
    }

    @Override
    protected void setEntityId(TbPenjualanEntity entity, String id) {
        entity.setNoPenjualan(id);
    }
}

