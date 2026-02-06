package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSparepartEntity;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbSparepartRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

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
        PanacheQuery<TbSparepartEntity> query;

        StringBuilder queryBuilder = new StringBuilder();
        java.util.Map<String, Object> params = new java.util.HashMap<>();

        if (pageRequest.getSearch() != null && !pageRequest.getSearch().isEmpty()) {
            queryBuilder.append("(lower(namaSparepart) like :search or lower(kodeSparepart) like :search)");
            params.put("search", "%" + pageRequest.getSearch().toLowerCase() + "%");
        }

        if (pageRequest.getSupplierId() != null) {
            if (queryBuilder.length() > 0) {
                queryBuilder.append(" and ");
            }
            queryBuilder.append("supplierId = :supplierId");
            params.put("supplierId", pageRequest.getSupplierId());
        }

        String queryString = queryBuilder.length() > 0 ? queryBuilder.toString() : "";

        if (pageRequest.getSortBy() != null && !pageRequest.getSortBy().isEmpty()) {
            Sort sort = pageRequest.isDescending()
                    ? Sort.descending(pageRequest.getSortBy())
                    : Sort.ascending(pageRequest.getSortBy());
            query = repository.find(queryString, sort, params);
        } else {
            query = repository.find(queryString, params);
        }

        long totalCount = query.count();
        List<TbSparepartEntity> rows = query.page(Page.of(pageRequest.getPage() - 1, pageRequest.getRowsPerPage()))
                .list();

        return new PageResponse<>(rows, pageRequest.getPage(), pageRequest.getRowsPerPage(), totalCount);
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
