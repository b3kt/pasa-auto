package com.github.b3kt.application.dto;

import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPembelianDetailEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPembelianEntity;

import java.util.List;

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
