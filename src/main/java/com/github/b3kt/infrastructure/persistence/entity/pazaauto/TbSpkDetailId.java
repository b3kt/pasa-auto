package com.github.b3kt.infrastructure.persistence.entity.pazaauto;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class TbSpkDetailId implements Serializable {

    @Column(name = "no_spk", length = 11)
    private String noSpk;

    @Column(name = "nama_jasa", length = 40)
    private String namaJasa;
}

