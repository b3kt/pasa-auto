package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbAbsensiConfigEntity;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbAbsensiConfigRepository;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class TbAbsensiConfigService extends AbstractCrudService<TbAbsensiConfigEntity, Long> {

    @Inject
    TbAbsensiConfigRepository repository;

    @Override
    protected PanacheRepositoryBase<TbAbsensiConfigEntity, Long> getRepository() {
        return repository;
    }

    @Override
    protected void setEntityId(TbAbsensiConfigEntity entity, Long id) {
        entity.setId(id);
    }

    /**
     * Get configuration value as string
     */
    public String getStringConfig(String key, String defaultValue) {
        TbAbsensiConfigEntity config = repository.find("configKey", key).firstResult();
        return config != null ? config.getConfigValue() : defaultValue;
    }

    /**
     * Get configuration value as integer
     */
    public int getIntegerConfig(String key, int defaultValue) {
        String value = getStringConfig(key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Get configuration value as boolean
     */
    public boolean getBooleanConfig(String key, boolean defaultValue) {
        String value = getStringConfig(key, String.valueOf(defaultValue));
        return Boolean.parseBoolean(value);
    }
}
