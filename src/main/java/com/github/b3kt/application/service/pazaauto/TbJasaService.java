package com.github.b3kt.application.service.pazaauto;

import java.util.List;

import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbJasaEntity;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbJasaRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
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
        PanacheQuery<TbJasaEntity> query;

        // Apply search filter if specified
        if (pageRequest.getSearch() != null && !pageRequest.getSearch().isEmpty()) {
            String searchPattern = "%" + pageRequest.getSearch().toLowerCase() + "%";
            query = repository.find(
                    "lower(namaJasa) like ?1",
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
                        "lower(namaJasa) like ?1",
                        sort,
                        searchPattern);
            } else {
                query = repository.findAll(sort);
            }
        }

        long totalCount = query.count();
        List<TbJasaEntity> rows = query.page(Page.of(pageRequest.getPage() - 1, pageRequest.getRowsPerPage())).list();

        return new PageResponse<>(rows, pageRequest.getPage(), pageRequest.getRowsPerPage(), totalCount);
    }
}
