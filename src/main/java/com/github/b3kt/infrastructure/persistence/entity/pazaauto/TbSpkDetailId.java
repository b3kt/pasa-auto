package com.github.b3kt.infrastructure.persistence.entity.pazaauto;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class TbSpkDetailId implements Serializable {

    @Column(name = "no_spk", length = 11)
    private String noSpk;

    @Column(name = "nama_jasa", length = 40)
    private String namaJasa;

    public TbSpkDetailId() {
    }

    public TbSpkDetailId(String noSpk, String namaJasa) {
        this.noSpk = noSpk;
        this.namaJasa = namaJasa;
    }

    public String getNoSpk() {
        return noSpk;
    }

    public void setNoSpk(String noSpk) {
        this.noSpk = noSpk;
    }

    public String getNamaJasa() {
        return namaJasa;
    }

    public void setNamaJasa(String namaJasa) {
        this.namaJasa = namaJasa;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TbSpkDetailId that = (TbSpkDetailId) o;
        return Objects.equals(noSpk, that.noSpk) && Objects.equals(namaJasa, that.namaJasa);
    }

    @Override
    public int hashCode() {
        return Objects.hash(noSpk, namaJasa);
    }
}

