package com.github.b3kt.application.service.customer;

import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPelangganEntity;

/**
 * Builds the stand-in {@code tb_pelanggan} row used when a customer account is linked before any
 * real customer/vehicle data is known (self-service "skip linking", or the first profile update of
 * an unlinked account). {@code nopol} and {@code merk} are NOT NULL on the table but are not part
 * of the self-service profile, so they get the {@code "-"} sentinel and are corrected later by staff.
 */
final class PelangganPlaceholder {

    static final String NOPOL = "-";
    static final String MERK = "-";

    private PelangganPlaceholder() {
    }

    static TbPelangganEntity create() {
        TbPelangganEntity pelanggan = new TbPelangganEntity();
        pelanggan.setNopol(NOPOL);
        pelanggan.setMerk(MERK);
        return pelanggan;
    }
}
