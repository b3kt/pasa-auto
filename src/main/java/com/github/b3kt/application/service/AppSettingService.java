package com.github.b3kt.application.service;

import com.github.b3kt.application.service.pazaauto.AbstractCrudService;
import com.github.b3kt.infrastructure.persistence.entity.AppSettingEntity;
import com.github.b3kt.infrastructure.persistence.repository.AppSettingRepository;
import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheKey;
import io.quarkus.cache.CacheResult;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;

/**
 * Generic, app-wide settings backed by {@link AppSettingEntity} - read on every request that
 * needs to check a runtime-toggleable flag, so reads are cached and writes explicitly invalidate.
 */
@ApplicationScoped
public class AppSettingService extends AbstractCrudService<AppSettingEntity, Long> {

    @Inject
    AppSettingRepository repository;

    @Override
    protected PanacheRepositoryBase<AppSettingEntity, Long> getRepository() {
        return repository;
    }

    @Override
    protected void setEntityId(AppSettingEntity entity, Long id) {
        entity.setId(id);
    }

    @CacheResult(cacheName = "app-settings-cache")
    public String getStringConfig(@CacheKey String key, String defaultValue) {
        AppSettingEntity config = repository.find("configKey", key).firstResult();
        return config != null ? config.getConfigValue() : defaultValue;
    }

    public boolean getBooleanConfig(String key, boolean defaultValue) {
        String value = getStringConfig(key, String.valueOf(defaultValue));
        return Boolean.parseBoolean(value);
    }

    @Transactional
    @CacheInvalidate(cacheName = "app-settings-cache")
    public void setBooleanConfig(@CacheKey String key, boolean value, String description, String updatedBy) {
        AppSettingEntity config = repository.find("configKey", key).firstResult();
        if (config == null) {
            config = new AppSettingEntity();
            config.setConfigKey(key);
            config.setConfigType("BOOLEAN");
            config.setDescription(description);
        }
        config.setConfigValue(String.valueOf(value));
        config.setUpdatedBy(updatedBy);
        config.setUpdatedAt(LocalDateTime.now());
        if (config.getId() == null) {
            repository.persist(config);
        }
    }
}
