package com.github.b3kt.application.dto.customer;

import com.github.b3kt.infrastructure.persistence.entity.subentity.SpkMekanik;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * The staff-only fields needed to turn a booking into a real {@code tb_spk} work order: mechanic
 * assignment and the true odometer reading at intake (a booking already stores what the customer
 * reported, but that is only a starting point).
 */
@Getter
@Setter
public class ConvertToSpkRequest {
    private Long mekanikId;
    private String namaKaryawan;
    private List<SpkMekanik> mekanikList;
    private Integer km;
}
