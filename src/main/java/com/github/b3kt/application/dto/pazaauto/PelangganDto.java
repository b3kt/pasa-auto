package com.github.b3kt.application.dto.pazaauto;

import java.time.LocalDate;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@RegisterForReflection
public class PelangganDto {

    @Schema(example = "10", description = "Unique identifier")
    private Long id;

    // Optional: a customer may own no vehicle (ADR-0011). Vehicles are managed through the ownership
    // endpoints; this field is the single-vehicle view of the primary vehicle.
    @Size(max = 10, message = "No Polisi must not exceed 10 characters")
    @Schema(example = "B1234XYZ", deprecated = true,
            description = "Plate of the customer's primary vehicle; manage vehicles via the ownership endpoints")
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

    // Deprecated landline (ADR-0012): always null in responses and ignored on write.
    @Size(max = 25, message = "Telepon must not exceed 25 characters")
    @Schema(deprecated = true, description = "Deprecated landline; always null, ignored on write")
    private String telepon;

    @Size(max = 50, message = "Merk must not exceed 50 characters")
    @Schema(example = "Toyota", deprecated = true, description = "Brand of the customer's primary vehicle")
    private String merk;

    @Size(max = 50, message = "Jenis must not exceed 50 characters")
    @Schema(example = "Avanza", deprecated = true, description = "Model/type of the customer's primary vehicle")
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

    @Size(max = 25, message = "No HP must not exceed 25 characters")
    @Schema(example = "081234567890", description = "Mobile phone, canonical national digits. For a customer "
            + "linked to a portal account this is the account's phone and is ignored on write (see contactFromAccount)")
    private String noHp;

    @Size(max = 25, message = "No Telepon must not exceed 25 characters")
    @Schema(deprecated = true, description = "Deprecated landline; always null, ignored on write")
    private String noTelepon;

    @Schema(example = "2020-01-15", description = "Customer registration date")
    private LocalDate tanggalJoin;

    @Schema(example = "STAFF", readOnly = true,
            description = "Record origin: STAFF (workshop data entry) or PORTAL (customer self-registration)")
    private String sumber;

    @Schema(readOnly = true, description = "True when noHp/email come from the customer's portal account "
            + "(tb_customer_accounts); staff edits to them are then ignored")
    private boolean contactFromAccount;
}
