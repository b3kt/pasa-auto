package com.github.b3kt.infrastructure.persistence.repository.pazaauto;

import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbBarangEntity;
import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheResult;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class TbBarangRepository implements PanacheRepositoryBase<TbBarangEntity, Long> {

    @CacheResult(cacheName = "barang-cache")
    public Optional<TbBarangEntity> findByIdCached(Long id) {
        return findByIdOptional(id);
    }

    @CacheResult(cacheName = "barang-cache")
    public List<TbBarangEntity> findAllCached() {
        return listAll();
    }

    @CacheInvalidate(cacheName = "barang-cache")
    public void invalidateBarangCache() {
        // Cache invalidation triggered on write operations
    }
}

