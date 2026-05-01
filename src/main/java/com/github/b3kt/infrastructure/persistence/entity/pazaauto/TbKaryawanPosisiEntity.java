package com.github.b3kt.infrastructure.persistence.entity.pazaauto;

import com.github.b3kt.infrastructure.persistence.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tb_karyawan_posisi")
@Getter
@Setter
@NoArgsConstructor
public class TbKaryawanPosisiEntity extends BaseEntity {

    @Column(name = "posisi", length = 20, nullable = false)
    private String posisi;

    @Column(name = "keterangan", length = 255)
    private String keterangan;
}
