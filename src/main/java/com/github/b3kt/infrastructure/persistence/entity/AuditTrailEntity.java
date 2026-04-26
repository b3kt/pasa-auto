package com.github.b3kt.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "tb_audit_trail", indexes = {
    @Index(name = "idx_audit_trail_table_name", columnList = "table_name"),
    @Index(name = "idx_audit_trail_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_trail_user_id", columnList = "user_id"),
    @Index(name = "idx_audit_trail_record_id", columnList = "record_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuditTrailEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "table_name", nullable = false, length = 255)
    private String tableName;

    @Column(name = "field_name", length = 255)
    private String fieldName;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", length = 255)
    private String username;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_value", columnDefinition = "jsonb")
    private Map<String, Object> beforeValue;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_value", columnDefinition = "jsonb")
    private Map<String, Object> afterValue;

    @Column(name = "record_id")
    private Long recordId;

    @Version
    @Column(name = "version")
    private Integer version;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;
}