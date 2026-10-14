package com.github.b3kt.application.dto.pazaauto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One selectable vehicle for the SPK nopol dropdown: the nopol together with its
 * current owner (from tb_pelanggan_kendaraan) and the vehicle master merk/jenis.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@RegisterForReflection
public class VehicleOptionDto {

    private String nopol;
    private String namaPelanggan;
    private String alamat;
    private String merk;
    private String jenis;
    private Long pelangganId;
    private Long kendaraanId;
}
