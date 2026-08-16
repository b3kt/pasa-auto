package com.github.b3kt.infrastructure.persistence.repository.pazaauto;

import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSparepartEntity;
import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheResult;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class TbSparepartRepository implements PanacheRepositoryBase<TbSparepartEntity, Long> {

    @CacheResult(cacheName = "sparepart-cache")
    public Optional<TbSparepartEntity> findByIdCached(Long id) {
        return findByIdOptional(id);
    }

    @CacheResult(cacheName = "sparepart-cache")
    public List<TbSparepartEntity> findAllCached() {
        return listAll();
    }

    @CacheInvalidate(cacheName = "sparepart-cache")
    public void invalidateSparepartCache() {
        // Cache invalidation triggered on write operations
    }
}
