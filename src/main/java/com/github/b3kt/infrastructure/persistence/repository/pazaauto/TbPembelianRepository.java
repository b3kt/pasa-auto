package com.github.b3kt.infrastructure.persistence.repository.pazaauto;

import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPembelianEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TbPembelianRepository implements PanacheRepositoryBase<TbPembelianEntity, Long> {

    public Integer findMaxNoUrut(java.time.LocalDateTime startOfDay, java.time.LocalDateTime endOfDay, String jenis) {
        return find("SELECT MAX(noUrut) FROM TbPembelianEntity WHERE tanggalPembelian >= ?1 AND tanggalPembelian < ?2 AND jenisPembelian = ?3",
                startOfDay, endOfDay, jenis).project(Integer.class).firstResult();
    }
}
