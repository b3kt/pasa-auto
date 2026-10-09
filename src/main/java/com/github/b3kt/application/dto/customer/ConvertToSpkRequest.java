package com.github.b3kt.application.dto.customer;

import lombok.Getter;
import lombok.Setter;

/**
 * The staff-only fields a booking doesn't carry, needed to turn it into a real {@code tb_spk}
 * work order: mechanic assignment and the odometer reading at intake.
 */
@Getter
@Setter
public class ConvertToSpkRequest {
    private Long mekanikId;
    private String namaKaryawan;
    private Integer km;
}
