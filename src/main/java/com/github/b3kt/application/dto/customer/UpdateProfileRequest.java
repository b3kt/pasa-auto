package com.github.b3kt.application.dto.customer;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** Editable subset of tb_pelanggan fields a linked customer may update themselves. */
@Getter
@Setter
public class UpdateProfileRequest {

    @NotBlank(message = "Name is required")
    private String namaPelanggan;

    private String alamat;

    /** Deprecated landline (ADR-0012): ignored. */
    @Deprecated
    private String telepon;

    /** Written to the account's phone (tb_customer_accounts.phone), which is also a login identifier. */
    private String noHp;

    /** Deprecated landline (ADR-0012): ignored. */
    @Deprecated
    private String noTelepon;

    /** Ignored: the contact email is the account's login email, which is not changed through the profile. */
    @Deprecated
    private String email;
    private String kota;
    private String kodePos;
    private String contactPerson;
}
