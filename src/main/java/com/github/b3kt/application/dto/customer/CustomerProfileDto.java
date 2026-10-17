package com.github.b3kt.application.dto.customer;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@RegisterForReflection
public class CustomerProfileDto {
    private Long accountId;
    private String email;
    private String phone;
    private String fullName;
    private boolean linked;

    // Populated only when linked - sourced from tb_pelanggan, editable via PUT /api/customer/profile
    private Long pelangganId;
    private String namaPelanggan;
    private String alamat;
    private String telepon;
    private String noHp;
    private String noTelepon;
    private String pelangganEmail;
    private String kota;
    private String kodePos;
    private String contactPerson;
}
