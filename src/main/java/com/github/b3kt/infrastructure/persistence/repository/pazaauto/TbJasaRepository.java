package com.github.b3kt.infrastructure.persistence.repository.pazaauto;

import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbJasaEntity;
import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheResult;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class TbJasaRepository implements PanacheRepositoryBase<TbJasaEntity, Long> {

    @CacheResult(cacheName = "jasa-cache")
    public Optional<TbJasaEntity> findByIdCached(Long id) {
        return findByIdOptional(id);
    }

    @CacheResult(cacheName = "jasa-cache")
    public List<TbJasaEntity> findAllCached() {
        return listAll();
    }

    @CacheInvalidate(cacheName = "jasa-cache")
    public void invalidateJasaCache() {
        // Cache invalidation triggered on write operations
    }
}

