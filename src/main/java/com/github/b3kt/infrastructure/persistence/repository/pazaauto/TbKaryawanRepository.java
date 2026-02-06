package com.github.b3kt.infrastructure.persistence.repository.pazaauto;

import java.util.List;
import java.util.Optional;

import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbKaryawanEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TbKaryawanRepository implements PanacheRepositoryBase<TbKaryawanEntity, Long> {
    public List<TbKaryawanEntity> findAllUnregistered() {
        return find("id NOT IN (SELECT u.karyawanId FROM UserEntity u WHERE u.karyawanId IS NOT NULL)").list();
    }

    public Optional<TbKaryawanEntity> findByUsername(String username) {
        return find("id IN (SELECT u.karyawanId FROM UserEntity u WHERE u.karyawanId IS NOT NULL AND u.username = ?1)",
                username).firstResultOptional();
    }
}
