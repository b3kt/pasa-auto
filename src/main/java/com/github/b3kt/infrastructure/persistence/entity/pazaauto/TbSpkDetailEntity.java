package com.github.b3kt.infrastructure.persistence.entity.pazaauto;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "tb_spk_detail")
@Getter
@Setter
public class TbSpkDetailEntity {

    @EmbeddedId
    private TbSpkDetailId id;

    @Column(name = "harga")
    private BigDecimal harga;

    @Column(name = "harga_master")
    private BigDecimal hargaMaster;

    @Column(name = "jumlah")
    private Integer jumlah;

    @Column(name = "keterangan", length = 500)
    private String keterangan;

    @Column(name = "id_jasa")
    private Long jasaId;

    @Column(name = "id_sparepart")
    private Long sparepartId;
}

