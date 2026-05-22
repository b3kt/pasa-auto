package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.application.helper.PageHelper;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSparepartEntity;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbSparepartRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class TbSparepartService extends AbstractCrudService<TbSparepartEntity, Long> {

    @Inject
    TbSparepartRepository repository;

    @Override
    protected PanacheRepositoryBase<TbSparepartEntity, Long> getRepository() {
        return repository;
    }

    @Override
    protected void setEntityId(TbSparepartEntity entity, Long id) {
        entity.setId(id);
    }

    @Override
    public PageResponse<TbSparepartEntity> findPaginated(PageRequest pageRequest) {
        StringBuilder queryBuilder = new StringBuilder();
        Map<String, Object> params = new HashMap<>();

        if (pageRequest.getSearch() != null && !pageRequest.getSearch().isEmpty()) {
            queryBuilder.append("(lower(namaSparepart) like :search or lower(kodeSparepart) like :search)");
            params.put("search", "%" + pageRequest.getSearch().toLowerCase() + "%");
        }

        if (pageRequest.getSupplierId() != null) {
            if (queryBuilder.length() > 0) queryBuilder.append(" and ");
            queryBuilder.append("supplierId = :supplierId");
            params.put("supplierId", pageRequest.getSupplierId());
        }

        String queryString = queryBuilder.length() > 0 ? queryBuilder.toString() : "";
        PanacheQuery<TbSparepartEntity> query = repository.find(queryString, params);
        return PageHelper.applyPagination(query, repository, pageRequest, queryString, params);
    }

    @jakarta.transaction.Transactional
    public void increaseStock(Long sparepartId, Integer quantity) {
        TbSparepartEntity sparepart = findById(sparepartId);
        Integer currentStock = sparepart.getStok() != null ? sparepart.getStok() : 0;
        sparepart.setStok(currentStock + quantity);
        repository.persist(sparepart);
    }

    @jakarta.transaction.Transactional
    public void decreaseStock(Long sparepartId, Integer quantity) {
        TbSparepartEntity sparepart = findById(sparepartId);
        Integer currentStock = sparepart.getStok() != null ? sparepart.getStok() : 0;
        int newStock = currentStock - quantity;
        if (newStock < 0) {
            throw new IllegalStateException("Cannot decrease stock below zero. Current stock: " + currentStock
                    + ", requested decrease: " + quantity);
        }
        sparepart.setStok(newStock);
        repository.persist(sparepart);
    }
}
