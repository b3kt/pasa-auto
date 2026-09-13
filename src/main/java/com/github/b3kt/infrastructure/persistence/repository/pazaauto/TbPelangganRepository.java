package com.github.b3kt.infrastructure.persistence.repository.pazaauto;

import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPelangganEntity;
import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheResult;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class TbPelangganRepository implements PanacheRepositoryBase<TbPelangganEntity, Long> {

    @CacheResult(cacheName = "pelanggan-cache")
    public Optional<TbPelangganEntity> findByIdCached(Long id) {
        return findByIdOptional(id);
    }

    @CacheResult(cacheName = "pelanggan-cache")
    public TbPelangganEntity findByNopolCached(String nopol) {
        return find("nopol", nopol).firstResult();
    }

    @CacheInvalidate(cacheName = "pelanggan-cache")
    public void invalidatePelangganCache() {
        // Cache invalidation triggered on write operations
    }

    @CacheResult(cacheName = "pelanggan-cache")
    public List<TbPelangganEntity> findAllCached() {
        return listAll();
    }
}

