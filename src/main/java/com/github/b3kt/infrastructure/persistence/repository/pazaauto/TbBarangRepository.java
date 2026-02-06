package com.github.b3kt.infrastructure.persistence.repository.pazaauto;

import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbBarangEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TbBarangRepository implements PanacheRepositoryBase<TbBarangEntity, Long> {
}

