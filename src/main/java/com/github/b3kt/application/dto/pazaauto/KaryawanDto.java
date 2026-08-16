package com.github.b3kt.application.dto.pazaauto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@RegisterForReflection
public class KaryawanDto {

    private Long id;

    @NotBlank(message = "Nama Karyawan is required")
    @Size(max = 30, message = "Nama Karyawan must not exceed 30 characters")
    private String namaKaryawan;

    @Size(max = 500, message = "Alamat must not exceed 500 characters")
    private String alamat;

    @Size(max = 15, message = "No Telepon must not exceed 15 characters")
    private String noTlpn;

    @Size(max = 10, message = "Bergabung must not exceed 10 characters")
    private String bergabung;

    private Integer posisiId;

    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @Size(max = 1, message = "Jenis Kelamin must be 1 character")
    private String jenisKelamin;

    @Size(max = 20, message = "No Telepon must not exceed 20 characters")
    private String noTelepon;

    private LocalDate tanggalBergabung;

    private Long idPosisi;

    private String namePosisi;

    private List<String> roles;
}