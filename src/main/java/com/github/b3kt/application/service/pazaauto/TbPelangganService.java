package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPelangganEntity;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbPelangganRepository;

import io.quarkus.cache.CacheResult;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class TbPelangganService extends AbstractCrudService<TbPelangganEntity, Long> {

    @Inject
    TbPelangganRepository repository;

    @Inject
    @io.quarkus.cache.CacheName("pelanggan-by-nopol")
    io.quarkus.cache.Cache cache;

    @Override
    protected PanacheRepositoryBase<TbPelangganEntity, Long> getRepository() {
        return repository;
    }

    @Override
    protected void setEntityId(TbPelangganEntity entity, Long id) {
        entity.setId(id);
    }

    @Override
    public PageResponse<TbPelangganEntity> findPaginated(PageRequest pageRequest) {
        PanacheQuery<TbPelangganEntity> query;

        // Apply search filter if specified
        if (pageRequest.getSearch() != null && !pageRequest.getSearch().isEmpty()) {
            String searchPattern = "%" + pageRequest.getSearch().toLowerCase() + "%";
            query = repository.find(
                    "lower(namaPelanggan) like ?1 or lower(nopol) like ?1 or lower(email) like ?1",
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
                        "lower(namaPelanggan) like ?1 or lower(nopol) like ?1 or lower(email) like ?1",
                        sort,
                        searchPattern);
            } else {
                query = repository.findAll(sort);
            }
        }

        // Get total count
        long totalCount = query.count();

        // Apply pagination
        List<TbPelangganEntity> rows = query.page(Page.of(pageRequest.getPage() - 1, pageRequest.getRowsPerPage()))
                .list();

        return new PageResponse<>(rows, pageRequest.getPage(), pageRequest.getRowsPerPage(), totalCount);
    }

    /**
     * Find pelanggan by nopol. If multiple records exist, return the last updated
     * one.
     *
     * @param nopol The vehicle registration number
     * @return The pelanggan entity or null if not found
     */
    @CacheResult(cacheName = "pelanggan-by-nopol")
    public TbPelangganEntity findByNopol(String nopol) {
        if (nopol == null || nopol.trim().isEmpty()) {
            return null;
        }

        return repository.find("nopol", Sort.descending("updatedAt"), nopol)
                .firstResult();
    }

    @Override
    @jakarta.transaction.Transactional
    public TbPelangganEntity update(Long id, TbPelangganEntity entity) {
        // Invalidate cache for old nopol
        TbPelangganEntity existing = findById(id);
        if (existing != null && existing.getNopol() != null) {
            cache.invalidate(existing.getNopol()).await().indefinitely();
        }

        // Invalidate cache for new nopol
        if (entity.getNopol() != null) {
            cache.invalidate(entity.getNopol()).await().indefinitely();
        }

        return super.update(id, entity);
    }
}
