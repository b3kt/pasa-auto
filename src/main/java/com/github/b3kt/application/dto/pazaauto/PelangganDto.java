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
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@RegisterForReflection
public class PelangganDto {

    @Schema(example = "10", description = "Unique identifier")
    private Long id;

    @NotBlank(message = "No Polisi is required")
    @Size(max = 10, message = "No Polisi must not exceed 10 characters")
    //@Pattern(regexp = "^[A-Z]\\d{1,4}[A-Z]{1,3}$", message = "Invalid No Polisi format")
    @Schema(example = "B1234XYZ", description = "Vehicle license plate (Indonesian format)")
    private String nopol;

    @NotBlank(message = "Nama Pelanggan is required")
    @Size(max = 100, message = "Nama Pelanggan must not exceed 100 characters")
    @Schema(example = "John Doe", description = "Customer full name")
    private String namaPelanggan;

    @Size(max = 500, message = "Alamat must not exceed 500 characters")
    @Schema(example = "Jl. Sudirman No. 123, Jakarta Selatan", description = "Customer address")
    private String alamat;

    @Size(max = 30, message = "Contact Person must not exceed 30 characters")
    @Schema(example = "Jane Smith", description = "Contact person name")
    private String contactPerson;

    @Size(max = 15, message = "Telepon must not exceed 15 characters")
    @Schema(example = "021-5551234", description = "Landline phone number")
    private String telepon;

    @NotBlank(message = "Merk is required")
    @Size(max = 50, message = "Merk must not exceed 50 characters")
    @Schema(example = "Toyota", description = "Vehicle brand")
    private String merk;

    @Size(max = 50, message = "Jenis must not exceed 50 characters")
    @Schema(example = "Avanza", description = "Vehicle model/type")
    private String jenis;

    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    @Schema(example = "john.doe@email.com", description = "Customer email address")
    private String email;

    @Size(max = 1, message = "Jenis Kelamin must be 1 character")
    @Schema(example = "L", description = "Gender: L (Male) or P (Female)")
    private String jenisKelamin;

    @Size(max = 500, message = "Keterangan must not exceed 500 characters")
    @Schema(example = "Loyal customer since 2020", description = "Additional notes")
    private String keterangan;

    @Size(max = 10, message = "Kode Pos must not exceed 10 characters")
    @Schema(example = "12410", description = "Postal code")
    private String kodePos;

    @Size(max = 100, message = "Kota must not exceed 100 characters")
    @Schema(example = "Jakarta", description = "City")
    private String kota;

    @Size(max = 20, message = "No HP must not exceed 20 characters")
    @Schema(example = "0812-3456-7890", description = "Mobile phone number")
    private String noHp;

    @Size(max = 20, message = "No Telepon must not exceed 20 characters")
    @Schema(example = "021-5555678", description = "Alternative phone number")
    private String noTelepon;

    @Schema(example = "2020-01-15", description = "Customer registration date")
    private LocalDate tanggalJoin;
}