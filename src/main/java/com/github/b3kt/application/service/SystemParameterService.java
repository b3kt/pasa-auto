package com.github.b3kt.application.service;

import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.application.service.pazaauto.AbstractCrudService;
import com.github.b3kt.infrastructure.persistence.entity.SystemParameterEntity;
import com.github.b3kt.infrastructure.persistence.repository.SystemParameterEntityRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class SystemParameterService extends AbstractCrudService<SystemParameterEntity, Long> {

    @Inject
    SystemParameterEntityRepository repository;

    @Override
    protected PanacheRepositoryBase<SystemParameterEntity, Long> getRepository() {
        return repository;
    }

    @Override
    protected void setEntityId(SystemParameterEntity entity, Long id) {
        entity.setId(id);
    }

    @Override
    public PageResponse<SystemParameterEntity> findPaginated(PageRequest pageRequest) {
        PanacheQuery<SystemParameterEntity> query;

        // Apply search filter if specified
        if (pageRequest.getSearch() != null && !pageRequest.getSearch().isEmpty()) {
            String searchPattern = "%" + pageRequest.getSearch().toLowerCase() + "%";
            query = repository.find(
                    "lower(name) like ?1 or lower(value) like ?1",
                    searchPattern);
        } else {
            query = repository.findAll();
        }

        // Apply sorting if specified
        if (pageRequest.getSortBy() != null && !pageRequest.getSortBy().isEmpty()) {
            Sort sort = pageRequest.isDescending()
                    ? Sort.descending(pageRequest.getSortBy())
                    : Sort.ascending(pageRequest.getSortBy());
            query = query.page(Page.of(0, Integer.MAX_VALUE));

            if (pageRequest.getSearch() != null && !pageRequest.getSearch().isEmpty()) {
                String searchPattern = "%" + pageRequest.getSearch().toLowerCase() + "%";
                query = repository.find(
                        "lower(name) like ?1 or lower(value) like ?1",
                        sort,
                        searchPattern);
            } else {
                query = repository.findAll(sort);
            }
        }

        long totalCount = query.count();
        List<SystemParameterEntity> rows = query.page(Page.of(pageRequest.getPage() - 1, pageRequest.getRowsPerPage()))
                .list();

        return new PageResponse<>(rows, pageRequest.getPage(), pageRequest.getRowsPerPage(), totalCount);
    }
}
