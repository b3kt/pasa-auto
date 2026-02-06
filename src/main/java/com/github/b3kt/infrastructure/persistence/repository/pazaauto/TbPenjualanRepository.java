package com.github.b3kt.infrastructure.persistence.repository.pazaauto;

import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPenjualanEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TbPenjualanRepository implements PanacheRepositoryBase<TbPenjualanEntity, String> {
}

