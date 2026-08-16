package com.github.b3kt.application.dto.pazaauto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@RegisterForReflection
public class SupplierDto {

    private Integer id;

    @NotBlank(message = "Nama Supplier is required")
    @Size(max = 100, message = "Nama Supplier must not exceed 100 characters")
    private String namaSupplier;

    @Size(max = 500, message = "Alamat must not exceed 500 characters")
    private String alamat;

    @Size(max = 13, message = "Telepon must not exceed 13 characters")
    private String telepon;

    @Size(max = 100, message = "Detail Supplier must not exceed 100 characters")
    private String detailSupplier;

    private LocalDateTime createdAt;

    @Size(max = 50, message = "Created By must not exceed 50 characters")
    private String createdBy;

    private LocalDateTime updatedAt;

    @Size(max = 50, message = "Updated By must not exceed 50 characters")
    private String updatedBy;

    private Integer version;

    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @Size(max = 500, message = "Keterangan must not exceed 500 characters")
    private String keterangan;

    @Size(max = 10, message = "Kode Pos must not exceed 10 characters")
    private String kodePos;

    @Size(max = 100, message = "Kontak Person must not exceed 100 characters")
    private String kontakPerson;

    @Size(max = 100, message = "Kota must not exceed 100 characters")
    private String kota;

    @Size(max = 20, message = "No HP Kontak must not exceed 20 characters")
    private String noHpKontak;

    @Size(max = 20, message = "No Telepon must not exceed 20 characters")
    private String noTelepon;
}