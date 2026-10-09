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
    private String telepon;
    private String noHp;
    private String noTelepon;
    private String email;
    private String kota;
    private String kodePos;
    private String contactPerson;
}
