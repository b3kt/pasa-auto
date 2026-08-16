package com.github.b3kt.application.dto.pazaauto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@RegisterForReflection
public class PelangganDto {

    private Long id;

    @NotBlank(message = "No Polisi is required")
    @Size(max = 10, message = "No Polisi must not exceed 10 characters")
    @Pattern(regexp = "^[A-Z]\d{1,4}[A-Z]{1,3}$", message = "Invalid No Polisi format")
    private String nopol;

    @NotBlank(message = "Nama Pelanggan is required")
    @Size(max = 100, message = "Nama Pelanggan must not exceed 100 characters")
    private String namaPelanggan;

    @Size(max = 500, message = "Alamat must not exceed 500 characters")
    private String alamat;

    @Size(max = 30, message = "Contact Person must not exceed 30 characters")
    private String contactPerson;

    @Size(max = 15, message = "Telepon must not exceed 15 characters")
    private String telepon;

    @NotBlank(message = "Merk is required")
    @Size(max = 50, message = "Merk must not exceed 50 characters")
    private String merk;

    @Size(max = 50, message = "Jenis must not exceed 50 characters")
    private String jenis;

    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @Size(max = 1, message = "Jenis Kelamin must be 1 character")
    private String jenisKelamin;

    @Size(max = 500, message = "Keterangan must not exceed 500 characters")
    private String keterangan;

    @Size(max = 10, message = "Kode Pos must not exceed 10 characters")
    private String kodePos;

    @Size(max = 100, message = "Kota must not exceed 100 characters")
    private String kota;

    @Size(max = 20, message = "No HP must not exceed 20 characters")
    private String noHp;

    @Size(max = 20, message = "No Telepon must not exceed 20 characters")
    private String noTelepon;

    private LocalDate tanggalJoin;
}