package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.infrastructure.persistence.entity.BaseEntity;
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
        T existing = getRepository().findByIdOptional(id)
                .orElseThrow(() -> new EntityNotFoundException("Entity not found with id: " + id));
        setEntityId(entity, id);
        carryOverVersion(existing, entity);
        return getRepository().getEntityManager().merge(entity);
    }

    /**
     * None of this app's DTOs round-trip the @Version value (clients never
     * receive it on read), so an incoming entity built from a DTO always has a
     * null version. Left as null, Hibernate's merge() reads that as a stale
     * write and rejects the update with "Row was already updated or deleted by
     * another transaction" — every time, for every entity. Carry the currently
     * persisted version forward instead of doing optimistic-lock validation the
     * API doesn't actually support end-to-end.
     * <p>
     * Trade-off: this makes update() last-write-wins rather than truly
     * optimistic-locked — a write that raced another request committed in
     * between is applied, not rejected, because we always re-read and stamp
     * the current version right before merge. That is not a regression: no
     * DTO has ever carried a real version for a client to send back, so no
     * working conflict detection existed to preserve (every update failed
     * unconditionally before this fix). Real optimistic locking would require
     * adding version to every DTO/mapper and having clients echo it back.
     */
    private void carryOverVersion(T existing, T entity) {
        if (existing instanceof BaseEntity existingBase && entity instanceof BaseEntity entityBase) {
            entityBase.setVersion(existingBase.getVersion());
        }
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
