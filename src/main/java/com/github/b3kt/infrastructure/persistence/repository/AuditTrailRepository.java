package com.github.b3kt.infrastructure.persistence.repository;

import com.github.b3kt.infrastructure.persistence.entity.AuditTrailEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class AuditTrailRepository implements PanacheRepository<AuditTrailEntity> {

    /**
     * tb_audit_trail is append-only and grows with every change to every audited table, so these
     * reads are capped: an unbounded list of it eventually exhausts the heap.
     */
    private static final Sort NEWEST_FIRST = Sort.by("timestamp").descending();

    public List<AuditTrailEntity> listRecent(int limit) {
        return findAll(NEWEST_FIRST).range(0, limit - 1).list();
    }

    public List<AuditTrailEntity> findByTableName(String tableName, int limit) {
        return find("tableName", NEWEST_FIRST, tableName).range(0, limit - 1).list();
    }

    public List<AuditTrailEntity> findByUserId(Long userId, int limit) {
        return find("userId", NEWEST_FIRST, userId).range(0, limit - 1).list();
    }

    public List<AuditTrailEntity> findByRecordId(Long recordId, int limit) {
        return find("recordId", NEWEST_FIRST, recordId).range(0, limit - 1).list();
    }

    public List<AuditTrailEntity> findByTableNameAndRecordId(String tableName, Long recordId, int limit) {
        return find("tableName = ?1 and recordId = ?2", NEWEST_FIRST, tableName, recordId)
                .range(0, limit - 1).list();
    }

    /** The usernames present in the audit trail, for the filter dropdown on the audit page. */
    public List<String> findDistinctUsernames() {
        return getEntityManager()
                .createQuery("select distinct a.username from AuditTrailEntity a "
                        + "where a.username is not null order by a.username", String.class)
                .getResultList();
    }

    public Optional<AuditTrailEntity> findLatestByRecordId(Long recordId) {
        return find("recordId = ?1 order by timestamp desc", recordId)
                .firstResultOptional();
    }
}