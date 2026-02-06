package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSupplierEntity;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbSupplierRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
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
        PanacheQuery<TbSupplierEntity> query;

        // Apply search filter if specified
        if (pageRequest.getSearch() != null && !pageRequest.getSearch().isEmpty()) {
            String searchPattern = "%" + pageRequest.getSearch().toLowerCase() + "%";
            query = repository.find(
                    "lower(namaSupplier) like ?1 or lower(email) like ?1",
                    searchPattern);
        } else {
            query = repository.findAll();
        }

        // Apply sorting if specified
        if (pageRequest.getSortBy() != null && !pageRequest.getSortBy().isEmpty()) {
            Sort sort = pageRequest.isDescending()
                    ? Sort.descending(pageRequest.getSortBy())
                    : Sort.ascending(pageRequest.getSortBy());
            query = query.page(Page.of(0, Integer.MAX_VALUE)); // Reset pagination for sorting

            // Re-apply query with sorting
            if (pageRequest.getSearch() != null && !pageRequest.getSearch().isEmpty()) {
                String searchPattern = "%" + pageRequest.getSearch().toLowerCase() + "%";
                query = repository.find(
                        "lower(namaSupplier) like ?1 or lower(email) like ?1",
                        sort,
                        searchPattern);
            } else {
                query = repository.findAll(sort);
            }
        }

        // Get total count
        long totalCount = query.count();

        // Apply pagination
        List<TbSupplierEntity> rows = query.page(Page.of(pageRequest.getPage() - 1, pageRequest.getRowsPerPage()))
                .list();

        return new PageResponse<>(rows, pageRequest.getPage(), pageRequest.getRowsPerPage(), totalCount);
    }
    public List<TbSupplierEntity> search(String search) {
        if (search == null || search.isEmpty()) {
            return repository.listAll();
        }
        String searchPattern = "%" + search.toLowerCase() + "%";
        return repository.find("lower(namaSupplier) like ?1 or lower(email) like ?1", Sort.by("namaSupplier"), searchPattern).list();
    }
}
