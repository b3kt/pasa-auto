package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.application.helper.PageHelper;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbJasaEntity;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbJasaRepository;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class TbJasaService extends AbstractCrudService<TbJasaEntity, Long> {

    @Inject
    TbJasaRepository repository;

    @Override
    protected PanacheRepositoryBase<TbJasaEntity, Long> getRepository() {
        return repository;
    }

    @Override
    protected void setEntityId(TbJasaEntity entity, Long id) {
        entity.setId(id);
    }

    @Override
    public PageResponse<TbJasaEntity> findPaginated(PageRequest pageRequest) {
        if (pageRequest.getSearch() != null && !pageRequest.getSearch().isEmpty()) {
            return PageHelper.paginate(repository, pageRequest,
                    "lower(namaJasa) like ?1",
                    "%" + pageRequest.getSearch().toLowerCase() + "%");
        }
        return PageHelper.findAll(repository, pageRequest);
    }
}
