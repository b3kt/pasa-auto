package com.github.b3kt.infrastructure.persistence.listener;

import com.github.b3kt.infrastructure.persistence.entity.BaseEntity;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@ApplicationScoped
@RequiredArgsConstructor
public class AuditListener {

    private final SecurityIdentity securityIdentity;

    @PrePersist
    public void prePersist(Object entity) {
        if (entity instanceof BaseEntity baseEntity) {
            LocalDateTime now = LocalDateTime.now();
            baseEntity.setCreatedAt(now);
            baseEntity.setUpdatedAt(now);

            if (baseEntity.getVersion() == null) {
                baseEntity.setVersion(0);
            }

            String username = getUsername();
            baseEntity.setCreatedBy(username);
            baseEntity.setUpdatedBy(username);
        }
    }

    @PreUpdate
    public void preUpdate(Object entity) {
        if (entity instanceof BaseEntity baseEntity) {
            baseEntity.setUpdatedAt(LocalDateTime.now());
            baseEntity.setUpdatedBy(getUsername());
        }
    }

    private String getUsername() {
        if (Objects.nonNull(securityIdentity) &&
            Objects.nonNull(securityIdentity.getPrincipal()) &&
            !securityIdentity.isAnonymous()) {
            return securityIdentity.getPrincipal().getName();
        }
        return "system";
    }
}
