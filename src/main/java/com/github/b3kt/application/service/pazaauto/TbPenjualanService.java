package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPenjualanEntity;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbPenjualanRepository;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

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

    public TbPenjualanEntity findByNoPenjualan(String noPenjualan) {
        return repository.findByNoPenjualan(noPenjualan);
    }

    @Transactional
    public TbPenjualanEntity createWithNoSpkValidation(TbPenjualanEntity entity) {
        // Check if noSpk already exists in penjualan
        TbPenjualanEntity existing = repository.find("noSpk", entity.getNoSpk()).firstResult();
        if (existing != null) {
            throw new IllegalArgumentException("No SPK " + entity.getNoSpk() + " already exists in penjualan records");
        }
        
        return create(entity);
    }

    @Transactional
    public TbPenjualanEntity updateWithNoSpkValidation(TbPenjualanEntity entity) {
        // Check if another record with same noSpk exists (excluding current record)
        TbPenjualanEntity existing = repository.find("noSpk = ?1 AND noPenjualan <> ?2", 
            entity.getNoSpk(), entity.getNoPenjualan()).firstResult();
        if (existing != null) {
            throw new IllegalArgumentException("No SPK " + entity.getNoSpk() + " already exists in other penjualan records");
        }
        
        return update(entity.getNoPenjualan(), entity);
    }
}

