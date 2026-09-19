package com.github.b3kt.infrastructure.persistence.entity.pazaauto;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import com.github.b3kt.infrastructure.persistence.entity.BaseEntity;

@Entity
@Table(name = "tb_kendaraan")
@Getter
@Setter
public class TbKendaraanEntity extends BaseEntity {

    @Column(name = "jenis", length = 50, nullable = false)
    private String jenis;

    @Column(name = "merk_id")
    private Long merkId;

    /**
     * @deprecated use {@link #merkId} (tb_merk_kendaraan). Kept in sync with the
     * merk master's nama until all consumers migrate; will be removed.
     */
    @Deprecated
    @Column(name = "merk", length = 50)
    private String merk;

    @Column(name = "keterangan", length = 500)
    private String keterangan;

    @Column(name = "model", length = 50)
    private String model;

}
