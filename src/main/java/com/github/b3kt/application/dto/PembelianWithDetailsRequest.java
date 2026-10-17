package com.github.b3kt.application.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPembelianDetailEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPembelianEntity;

import java.util.List;

@RegisterForReflection
public class PembelianWithDetailsRequest {
    private TbPembelianEntity pembelian;
    private List<TbPembelianDetailEntity> details;

    public TbPembelianEntity getPembelian() {
        return pembelian;
    }

    public void setPembelian(TbPembelianEntity pembelian) {
        this.pembelian = pembelian;
    }

    public List<TbPembelianDetailEntity> getDetails() {
        return details;
    }

    public void setDetails(List<TbPembelianDetailEntity> details) {
        this.details = details;
    }
}
