package com.github.b3kt.infrastructure.persistence.repository.pazaauto;

import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbKaryawanEntity;
import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheResult;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class TbKaryawanRepository implements PanacheRepositoryBase<TbKaryawanEntity, Long> {

    @CacheResult(cacheName = "karyawan-cache")
    public Optional<TbKaryawanEntity> findByIdCached(Long id) {
        return findByIdOptional(id);
    }

    @CacheResult(cacheName = "karyawan-cache")
    public List<TbKaryawanEntity> findAllCached() {
        return listAll();
    }

    public List<TbKaryawanEntity> findAllUnregistered() {
        return find("id NOT IN (SELECT u.karyawanId FROM UserEntity u WHERE u.karyawanId IS NOT NULL)").list();
    }

    public Optional<TbKaryawanEntity> findByUsername(String username) {
        return find("id IN (SELECT u.karyawanId FROM UserEntity u WHERE u.karyawanId IS NOT NULL AND u.username = ?1)",
                username).firstResultOptional();
    }

    @CacheInvalidate(cacheName = "karyawan-cache")
    public void invalidateKaryawanCache() {
        // Cache invalidation triggered on write operations
    }
}
