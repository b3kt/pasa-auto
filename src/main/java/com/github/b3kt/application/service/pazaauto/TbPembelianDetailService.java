package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPembelianDetailEntity;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbPembelianDetailRepository;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class TbPembelianDetailService extends AbstractCrudService<TbPembelianDetailEntity, Long> {

    @Inject
    TbPembelianDetailRepository repository;

    @Override
    protected PanacheRepositoryBase<TbPembelianDetailEntity, Long> getRepository() {
        return repository;
    }

    @Override
    protected void setEntityId(TbPembelianDetailEntity entity, Long id) {
        entity.setId(id);
    }

    public List<TbPembelianDetailEntity> findByPembelianId(Long pembelianId) {
        return repository.findByPembelianId(pembelianId);
    }

    @Transactional
    public void deleteByPembelianId(Long pembelianId) {
        repository.deleteByPembelianId(pembelianId);
    }

    @Transactional
    public void saveAll(List<TbPembelianDetailEntity> details) {
        for (TbPembelianDetailEntity detail : details) {
            repository.persist(detail);
        }
    }
}
