package com.github.b3kt.infrastructure.persistence.repository.pazaauto;

import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPembelianDetailEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class TbPembelianDetailRepository implements PanacheRepositoryBase<TbPembelianDetailEntity, Long> {

    public List<TbPembelianDetailEntity> findByPembelianId(Long pembelianId) {
        return list("pembelianId", pembelianId);
    }

    public long deleteByPembelianId(Long pembelianId) {
        return delete("pembelianId", pembelianId);
    }
}
