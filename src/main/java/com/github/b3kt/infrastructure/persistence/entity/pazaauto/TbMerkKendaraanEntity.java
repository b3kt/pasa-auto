package com.github.b3kt.infrastructure.persistence.entity.pazaauto;

import com.github.b3kt.infrastructure.persistence.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tb_merk_kendaraan")
@Getter
@Setter
@NoArgsConstructor
public class TbMerkKendaraanEntity extends BaseEntity {

    @Column(name = "nama", length = 50, nullable = false)
    private String nama;

    @Column(name = "keterangan", length = 500)
    private String keterangan;
}
