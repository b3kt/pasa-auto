package com.github.b3kt.infrastructure.persistence.repository.pazaauto;

import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbKendaraanEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TbKendaraanRepository implements PanacheRepositoryBase<TbKendaraanEntity, Long> {

    public java.util.List<String> findDistinctMerk() {
        return find("SELECT DISTINCT merk FROM TbKendaraanEntity WHERE merk IS NOT NULL ORDER BY merk").project(String.class).list();
    }

    public java.util.List<String> findDistinctJenis() {
        return find("SELECT DISTINCT jenis FROM TbKendaraanEntity WHERE jenis IS NOT NULL ORDER BY jenis").project(String.class).list();
    }

    public java.util.List<String> findDistinctJenisByMerk(String merk) {
        return find("SELECT DISTINCT jenis FROM TbKendaraanEntity WHERE jenis IS NOT NULL AND lower(merk) = lower(?1) ORDER BY jenis", merk).project(String.class).list();
    }
}

