package com.github.b3kt.infrastructure.persistence.repository.pazaauto;

import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbKendaraanEntity;
import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheResult;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class TbKendaraanRepository implements PanacheRepositoryBase<TbKendaraanEntity, Long> {

    @CacheResult(cacheName = "kendaraan-cache")
    public Optional<TbKendaraanEntity> findByIdCached(Long id) {
        return findByIdOptional(id);
    }

    @CacheResult(cacheName = "kendaraan-cache")
    public List<TbKendaraanEntity> findAllCached() {
        return listAll();
    }

    public java.util.List<String> findDistinctMerk() {
        return find("SELECT DISTINCT merk FROM TbKendaraanEntity WHERE merk IS NOT NULL ORDER BY merk").project(String.class).list();
    }

    public java.util.List<String> findDistinctJenis() {
        return find("SELECT DISTINCT jenis FROM TbKendaraanEntity WHERE jenis IS NOT NULL ORDER BY jenis").project(String.class).list();
    }

    public java.util.List<String> findDistinctJenisByMerk(String merk) {
        return find("SELECT DISTINCT jenis FROM TbKendaraanEntity WHERE jenis IS NOT NULL AND lower(merk) = lower(?1) ORDER BY jenis", merk).project(String.class).list();
    }

    @CacheInvalidate(cacheName = "kendaraan-cache")
    public void invalidateKendaraanCache() {
        // Cache invalidation triggered on write operations
    }
}

