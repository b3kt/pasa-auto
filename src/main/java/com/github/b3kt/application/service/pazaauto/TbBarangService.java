package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbBarangEntity;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbBarangRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
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
        PanacheQuery<TbBarangEntity> query;

        // Build query with filters
        String queryString = "1=1";
        Object[] params = new Object[0];
        int paramIndex = 1;

        // Apply search filter if specified
        if (pageRequest.getSearch() != null && !pageRequest.getSearch().isEmpty()) {
            String searchPattern = "%" + pageRequest.getSearch().toLowerCase() + "%";
            queryString += " and (lower(kodeBarang) like ?" + paramIndex + " or lower(namaBarang) like ?" + paramIndex
                    + ")";
            params = new Object[] { searchPattern };
            paramIndex++;
        }

        // Apply status filter if specified
        if (pageRequest.getStatusFilter() != null && !pageRequest.getStatusFilter().isEmpty()) {
            String[] statuses = pageRequest.getStatusFilter().split(",");
            StringBuilder statusQuery = new StringBuilder(" and (");

            for (int i = 0; i < statuses.length; i++) {
                if (i > 0) {
                    statusQuery.append(" or ");
                }
                String status = statuses[i].trim();
                if ("AVAILABLE".equals(status)) {
                    statusQuery.append("stok > 0");
                } else if ("OUT_OF_STOCK".equals(status)) {
                    statusQuery.append("stok <= 0");
                }
            }
            statusQuery.append(")");
            queryString += statusQuery.toString();
        }

        // Create query
        if (params.length > 0) {
            query = repository.find(queryString, params);
        } else {
            query = repository.find(queryString);
        }

        // Apply sorting if specified
        if (pageRequest.getSortBy() != null && !pageRequest.getSortBy().isEmpty()) {
            Sort sort = pageRequest.isDescending()
                    ? Sort.descending(pageRequest.getSortBy())
                    : Sort.ascending(pageRequest.getSortBy());

            // Re-apply query with sorting
            if (params.length > 0) {
                query = repository.find(queryString, sort, params);
            } else {
                query = repository.find(queryString, sort);
            }
        }

        // Get total count
        long totalCount = query.count();

        // Apply pagination
        List<TbBarangEntity> rows = query.page(Page.of(pageRequest.getPage() - 1, pageRequest.getRowsPerPage())).list();

        return new PageResponse<>(rows, pageRequest.getPage(), pageRequest.getRowsPerPage(), totalCount);
    }
    public List<TbBarangEntity> search(String search) {
        if (search == null || search.isEmpty()) {
            return repository.listAll();
        }
        String searchPattern = "%" + search.toLowerCase() + "%";
        return repository.find("lower(kodeBarang) like ?1 or lower(namaBarang) like ?1", Sort.by("namaBarang"), searchPattern).list();
    }
}
