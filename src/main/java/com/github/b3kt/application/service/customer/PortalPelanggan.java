package com.github.b3kt.application.service.customer;

import com.github.b3kt.domain.model.pazaauto.Pelanggan;
import com.github.b3kt.infrastructure.persistence.entity.customer.CustomerAccountEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPelangganEntity;

/**
 * Builds the {@code tb_pelanggan} record of a customer who registered through the portal before any
 * workshop record is known (self-service "skip linking", first profile update, first vehicle).
 * It is an ordinary customer record marked {@code sumber = PORTAL} and owning no vehicle yet;
 * it replaces the former {@code "-"} placeholder (ADR-0011).
 */
final class PortalPelanggan {

    private PortalPelanggan() {
    }

    /**
     * Seeds the record's name from the account. Phone and email are not copied: for a linked customer
     * they are read from the account itself (ADR-0012).
     */
    static TbPelangganEntity create(CustomerAccountEntity account) {
        TbPelangganEntity pelanggan = new TbPelangganEntity();
        pelanggan.setSumber(Pelanggan.SUMBER_PORTAL);
        pelanggan.setNamaPelanggan(account.getFullName());
        return pelanggan;
    }
}
