package com.github.b3kt.infrastructure.persistence.repository;

import com.github.b3kt.infrastructure.persistence.entity.AuditTrailEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class AuditTrailRepository implements PanacheRepository<AuditTrailEntity> {

    public List<AuditTrailEntity> findByTableName(String tableName) {
        return list("tableName", tableName);
    }

    public List<AuditTrailEntity> findByUserId(Long userId) {
        return list("userId", userId);
    }

    public List<AuditTrailEntity> findByRecordId(Long recordId) {
        return list("recordId", recordId);
    }

    public List<AuditTrailEntity> findByTableNameAndRecordId(String tableName, Long recordId) {
        return list("tableName = ?1 and recordId = ?2", tableName, recordId);
    }

    public Optional<AuditTrailEntity> findLatestByRecordId(Long recordId) {
        return find("recordId = ?1 order by timestamp desc", recordId)
                .firstResultOptional();
    }
}