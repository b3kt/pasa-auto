package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbKendaraanEntity;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbKendaraanRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
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
        PanacheQuery<TbKendaraanEntity> query;

        if (pageRequest.getSearch() != null && !pageRequest.getSearch().isEmpty()) {
            String searchPattern = "%" + pageRequest.getSearch().toLowerCase() + "%";
            query = repository.find(
                    "lower(jenis) like ?1 or lower(merk) like ?1",
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
                        "lower(jenis) like ?1 or lower(merk) like ?1",
                        sort,
                        searchPattern);
            } else {
                query = repository.findAll(sort);
            }
        }

        long totalCount = query.count();
        List<TbKendaraanEntity> rows = query.page(Page.of(pageRequest.getPage() - 1, pageRequest.getRowsPerPage()))
                .list();

        return new PageResponse<>(rows, pageRequest.getPage(), pageRequest.getRowsPerPage(), totalCount);
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
