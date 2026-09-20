package com.github.b3kt.application.service;

import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.infrastructure.persistence.entity.AuditTrailEntity;
import com.github.b3kt.infrastructure.persistence.repository.AuditTrailRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class AuditTrailService {

    @Inject
    AuditTrailRepository repository;

    @Transactional
    public void record(AuditTrailEntity auditTrail) {
        auditTrail.setTimestamp(LocalDateTime.now());
        repository.persist(auditTrail);
    }

    public List<AuditTrailEntity> findByTableName(String tableName, int limit) {
        return repository.findByTableName(tableName, limit);
    }

    public List<AuditTrailEntity> findByUserId(Long userId, int limit) {
        return repository.findByUserId(userId, limit);
    }

    public List<AuditTrailEntity> findByRecordId(Long recordId, int limit) {
        return repository.findByRecordId(recordId, limit);
    }

    public List<AuditTrailEntity> findByTableNameAndRecordId(String tableName, Long recordId, int limit) {
        return repository.findByTableNameAndRecordId(tableName, recordId, limit);
    }

    public List<AuditTrailEntity> findRecent(int limit) {
        return repository.listRecent(limit);
    }

    public List<String> findDistinctUsernames() {
        return repository.findDistinctUsernames();
    }

    public PageResponse<AuditTrailEntity> findPaginated(PageRequest pageRequest) {
        String searchPattern = (pageRequest.getSearch() != null && !pageRequest.getSearch().isEmpty())
                ? "%" + pageRequest.getSearch().toLowerCase() + "%"
                : null;

        Sort sort;
        if (pageRequest.getSortBy() != null && !pageRequest.getSortBy().isEmpty()) {
            sort = pageRequest.isDescending()
                    ? Sort.by(pageRequest.getSortBy()).descending()
                    : Sort.by(pageRequest.getSortBy()).ascending();
        } else {
            sort = Sort.by("timestamp").descending();
        }

        PanacheQuery<AuditTrailEntity> query;
        if (searchPattern != null) {
            query = repository.find(
                    "lower(tableName) like ?1 or lower(username) like ?1 or lower(action) like ?1",
                    sort,
                    searchPattern);
        } else {
            query = repository.findAll(sort);
        }

        long totalCount = query.count();
        List<AuditTrailEntity> rows = query.page(Page.of(pageRequest.getPage() - 1, pageRequest.getRowsPerPage()))
                .list();

        return new PageResponse<>(rows, pageRequest.getPage(), pageRequest.getRowsPerPage(), totalCount);
    }

    public PanacheRepositoryBase<AuditTrailEntity, Long> getRepository() {
        return repository;
    }
}