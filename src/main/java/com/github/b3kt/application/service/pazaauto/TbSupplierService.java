package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.application.helper.PageHelper;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSupplierEntity;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbSupplierRepository;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class TbSupplierService extends AbstractCrudService<TbSupplierEntity, Integer> {

    @Inject
    TbSupplierRepository repository;

    @Override
    protected PanacheRepositoryBase<TbSupplierEntity, Integer> getRepository() {
        return repository;
    }

    @Override
    protected void setEntityId(TbSupplierEntity entity, Integer id) {
        entity.setId(id);
    }

    @Override
    public PageResponse<TbSupplierEntity> findPaginated(PageRequest pageRequest) {
        if (pageRequest.getSearch() != null && !pageRequest.getSearch().isEmpty()) {
            return PageHelper.paginate(repository, pageRequest,
                    "lower(namaSupplier) like ?1 or lower(email) like ?1",
                    "%" + pageRequest.getSearch().toLowerCase() + "%");
        }
        return PageHelper.findAll(repository, pageRequest);
    }
    public List<TbSupplierEntity> search(String search) {
        if (search == null || search.isEmpty()) {
            return repository.listAll();
        }
        String searchPattern = "%" + search.toLowerCase() + "%";
        return repository.find("lower(namaSupplier) like ?1 or lower(email) like ?1", Sort.by("namaSupplier"), searchPattern).list();
    }
}
