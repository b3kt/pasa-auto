package com.github.b3kt.application.dto;

import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPembelianDetailEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPembelianEntity;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PembelianWithDetailsRequest {
    private TbPembelianEntity pembelian;
    private List<TbPembelianDetailEntity> details;
}
