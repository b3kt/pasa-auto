package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.application.helper.PageHelper;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbBarangEntity;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbBarangRepository;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class TbBarangService extends AbstractCrudService<TbBarangEntity, Long> {

    @Inject
    TbBarangRepository repository;

    @Override
    protected PanacheRepositoryBase<TbBarangEntity, Long> getRepository() {
        return repository;
    }

    @Override
    protected void setEntityId(TbBarangEntity entity, Long id) {
        entity.setId(id);
    }

    @Override
    public PageResponse<TbBarangEntity> findPaginated(PageRequest pageRequest) {
        StringBuilder queryString = new StringBuilder("1=1");
        Object[] params = new Object[0];

        if (pageRequest.getSearch() != null && !pageRequest.getSearch().isEmpty()) {
            String searchPattern = "%" + pageRequest.getSearch().toLowerCase() + "%";
            queryString.append(" and (lower(kodeBarang) like ?1 or lower(namaBarang) like ?1)");
            params = new Object[] { searchPattern };
        }

        if (pageRequest.getStatusFilter() != null && !pageRequest.getStatusFilter().isEmpty()) {
            String[] statuses = pageRequest.getStatusFilter().split(",");
            StringBuilder statusQuery = new StringBuilder(" and (");
            for (int i = 0; i < statuses.length; i++) {
                if (i > 0) statusQuery.append(" or ");
                String status = statuses[i].trim();
                if ("AVAILABLE".equals(status)) {
                    statusQuery.append("stok > 0");
                } else if ("OUT_OF_STOCK".equals(status)) {
                    statusQuery.append("stok <= 0");
                }
            }
            statusQuery.append(")");
            queryString.append(statusQuery);
        }

        return PageHelper.paginate(repository, pageRequest, queryString.toString(), params);
    }
    public List<TbBarangEntity> search(String search) {
        if (search == null || search.isEmpty()) {
            return repository.listAll();
        }
        String searchPattern = "%" + search.toLowerCase() + "%";
        return repository.find("lower(kodeBarang) like ?1 or lower(namaBarang) like ?1", Sort.by("namaBarang"), searchPattern).list();
    }
}
