package com.github.b3kt.application.service;

import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.application.service.pazaauto.AbstractCrudService;
import com.github.b3kt.infrastructure.persistence.entity.PermissionEntity;
import com.github.b3kt.infrastructure.persistence.repository.PermissionEntityRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class PermissionService extends AbstractCrudService<PermissionEntity, Long> {

    @Inject
    PermissionEntityRepository repository;

    @Override
    protected PanacheRepositoryBase<PermissionEntity, Long> getRepository() {
        return repository;
    }

    @Override
    protected void setEntityId(PermissionEntity entity, Long id) {
        entity.setId(id);
    }

    @Override
    public PageResponse<PermissionEntity> findPaginated(PageRequest pageRequest) {
        PanacheQuery<PermissionEntity> query;

        if (pageRequest.getSearch() != null && !pageRequest.getSearch().isEmpty()) {
            String searchPattern = "%" + pageRequest.getSearch().toLowerCase() + "%";
            query = repository.find(
                    "lower(name) like ?1 or lower(description) like ?1 or lower(resource) like ?1 or lower(action) like ?1",
                    searchPattern);
        } else {
            query = repository.findAll();
        }

        if (pageRequest.getSortBy() != null && !pageRequest.getSortBy().isEmpty()) {
            Sort sort = pageRequest.isDescending()
                    ? Sort.descending(pageRequest.getSortBy())
                    : Sort.ascending(pageRequest.getSortBy());
            query = query.page(Page.of(0, Integer.MAX_VALUE));

            if (pageRequest.getSearch() != null && !pageRequest.getSearch().isEmpty()) {
                String searchPattern = "%" + pageRequest.getSearch().toLowerCase() + "%";
                query = repository.find(
                        "lower(name) like ?1 or lower(description) like ?1 or lower(resource) like ?1 or lower(action) like ?1",
                        sort,
                        searchPattern);
            } else {
                query = repository.findAll(sort);
            }
        }

        long totalCount = query.count();
        List<PermissionEntity> rows = query.page(Page.of(pageRequest.getPage() - 1, pageRequest.getRowsPerPage()))
                .list();

        return new PageResponse<>(rows, pageRequest.getPage(), pageRequest.getRowsPerPage(), totalCount);
    }
}
