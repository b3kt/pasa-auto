package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.application.helper.PageHelper;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbKendaraanEntity;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbKendaraanRepository;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class TbKendaraanService extends AbstractCrudService<TbKendaraanEntity, Long> {

    @Inject
    TbKendaraanRepository repository;

    @Override
    protected PanacheRepositoryBase<TbKendaraanEntity, Long> getRepository() {
        return repository;
    }

    @Override
    protected void setEntityId(TbKendaraanEntity entity, Long id) {
        entity.setId(id);
    }

    @Override
    public PageResponse<TbKendaraanEntity> findPaginated(PageRequest pageRequest) {
        if (pageRequest.getSearch() != null && !pageRequest.getSearch().isEmpty()) {
            return PageHelper.paginate(repository, pageRequest,
                    "lower(jenis) like ?1 or lower(merk) like ?1",
                    "%" + pageRequest.getSearch().toLowerCase() + "%");
        }
        return PageHelper.findAll(repository, pageRequest);
    }

    public List<String> findDistinctMerks() {
        return repository.findDistinctMerk();
    }

    public List<String> findDistinctJenis() {
        return repository.findDistinctJenis();
    }

    public List<String> findDistinctJenisByMerk(String merk) {
        return repository.findDistinctJenisByMerk(merk);
    }
}
