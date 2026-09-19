package com.github.b3kt.application.dto.pazaauto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * One ownership row: which customer owns which nopol (vehicle instance)
 * and the vehicle master (merk/jenis/model) it references.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@RegisterForReflection
public class KendaraanOwnershipDto {

    private Long id;

    private String nopol;

    private Long idKendaraan;
    private String merk;
    private String jenis;
    private String model;

    private Long idPelanggan;
    private String namaPelanggan;
    private String alamatPelanggan;
    private String noHpPelanggan;

    private LocalDate tanggalMulai;
    private LocalDate tanggalAkhir;
    private boolean current;

    private String keterangan;
}