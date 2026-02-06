package com.github.b3kt.infrastructure.persistence.entity.pazaauto;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import com.github.b3kt.infrastructure.persistence.entity.BaseEntity;

@Entity
@Table(name = "tb_jasa")
@Getter
@Setter
public class TbJasaEntity extends BaseEntity {

    @Column(name = "nama_jasa", length = 100, nullable = false)
    private String namaJasa;

    @Column(name = "harga_jasa")
    private Integer hargaJasa;

    @Column(name = "deskripsi", length = 500)
    private String deskripsi;

    @Column(name = "estimasi_waktu")
    private Integer estimasiWaktu;

}
