package com.github.b3kt.infrastructure.persistence.repository.pazaauto;

import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbMerkKendaraanEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class TbMerkKendaraanRepository implements PanacheRepositoryBase<TbMerkKendaraanEntity, Long> {

    public static String normalizeNama(String nama) {
        return nama == null ? null : nama.trim().toUpperCase();
    }

    public Optional<TbMerkKendaraanEntity> findByNamaIgnoreCase(String nama) {
        if (nama == null || nama.trim().isEmpty()) {
            return Optional.empty();
        }
        return find("lower(nama) = lower(?1)", nama.trim()).firstResultOptional();
    }

    public TbMerkKendaraanEntity findOrCreateByNama(String nama) {
        return findByNamaIgnoreCase(nama).orElseGet(() -> {
            TbMerkKendaraanEntity entity = new TbMerkKendaraanEntity();
            entity.setNama(normalizeNama(nama));
            return getEntityManager().merge(entity);
        });
    }

    public List<String> findAllNama() {
        return find("SELECT nama FROM TbMerkKendaraanEntity ORDER BY nama").project(String.class).list();
    }
}
