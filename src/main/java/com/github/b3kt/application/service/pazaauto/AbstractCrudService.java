package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Generic CRUD service that delegates to a Panache repository.
 *
 * @param <T>  entity type
 * @param <ID> identifier type
 */
@Slf4j
public abstract class AbstractCrudService<T, ID> {

    protected abstract PanacheRepositoryBase<T, ID> getRepository();

    protected abstract void setEntityId(T entity, ID id);

    protected void setRelationships(T entity) {
    }

    public List<T> findAll() {
        return getRepository().listAll();
    }

    public PageResponse<T> findPaginated(PageRequest pageRequest) {
        PanacheQuery<T> query = getRepository().findAll();

        // Apply sorting if specified
        if (pageRequest.getSortBy() != null && !pageRequest.getSortBy().isEmpty()) {
            Sort sort = pageRequest.isDescending()
                    ? Sort.descending(pageRequest.getSortBy())
                    : Sort.ascending(pageRequest.getSortBy());
            query = getRepository().findAll(sort);
        }

        // Get total count
        long totalCount = query.count();

        // Apply pagination
        List<T> rows = query.page(Page.of(pageRequest.getPage() - 1, pageRequest.getRowsPerPage())).list();

        return new PageResponse<>(rows, pageRequest.getPage(), pageRequest.getRowsPerPage(), totalCount);
    }

    public T findById(ID id) {
        return getRepository().findByIdOptional(id)
                .orElseThrow(() -> new EntityNotFoundException("Entity not found with id: " + id));
    }

    @Transactional
    public T create(T entity) {
        getRepository().persist(entity);
        return entity;
    }

    @Transactional
    public T update(ID id, T entity) {
        if (!getRepository().findByIdOptional(id).isPresent()) {
            throw new EntityNotFoundException("Entity not found with id: " + id);
        }
        setEntityId(entity, id);
        return getRepository().getEntityManager().merge(entity);
    }

    @Transactional
    public void delete(ID id) {
        try {
            boolean deleted = getRepository().deleteById(id);
            log.debug("Entity deleted with id: {}: {}", id, deleted);
        } catch (Exception e) {
            throw new EntityNotFoundException("Entity not found with id: " + id);
        }
    }
}
