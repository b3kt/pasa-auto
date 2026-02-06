package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSpkDetailEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSpkDetailId;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbSpkDetailRepository;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class TbSpkDetailService extends AbstractCrudService<TbSpkDetailEntity, TbSpkDetailId> {

    @Inject
    TbSpkDetailRepository repository;

    @Override
    protected PanacheRepositoryBase<TbSpkDetailEntity, TbSpkDetailId> getRepository() {
        return repository;
    }

    @Override
    protected void setEntityId(TbSpkDetailEntity entity, TbSpkDetailId id) {
        entity.setId(id);
    }
}

