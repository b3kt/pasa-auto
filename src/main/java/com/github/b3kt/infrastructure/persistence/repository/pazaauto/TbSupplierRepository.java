package com.github.b3kt.infrastructure.persistence.repository.pazaauto;

import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSupplierEntity;
import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheResult;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class TbSupplierRepository implements PanacheRepositoryBase<TbSupplierEntity, Integer> {

    @CacheResult(cacheName = "supplier-cache")
    public Optional<TbSupplierEntity> findByIdCached(Integer id) {
        return findByIdOptional(id);
    }

    @CacheResult(cacheName = "supplier-cache")
    public List<TbSupplierEntity> findAllCached() {
        return listAll();
    }

    @CacheInvalidate(cacheName = "supplier-cache")
    public void invalidateSupplierCache() {
        // Cache invalidation triggered on write operations
    }
}
