package com.github.b3kt.infrastructure.persistence.entity.pazaauto;

import com.github.b3kt.domain.model.pazaauto.Pelanggan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TbPelangganEntityTest {

    @Test
    @DisplayName("normalizes the mobile number and leaves the deprecated landline untouched")
    void normalizesMobileOnly() {
        TbPelangganEntity e = new TbPelangganEntity();
        e.setNoHp("+62 812-3456-7890");
        e.setNoTelepon("(021) 555 1234");
        e.setTelepon("legacy");

        e.normalizeContacts();

        assertEquals("081234567890", e.getNoHp());
        // Not written by the application any more (insertable/updatable = false): no rewriting either.
        assertEquals("(021) 555 1234", e.getNoTelepon());
        assertEquals("legacy", e.getTelepon());
    }

    @Test
    @DisplayName("a blank mobile number is stored as null")
    void blankMobileIsNull() {
        TbPelangganEntity e = new TbPelangganEntity();
        e.setNoHp("  ");

        e.normalizeContacts();

        assertNull(e.getNoHp());
    }

    @Test
    @DisplayName("defaults sumber to STAFF")
    void defaultsSumber() {
        TbPelangganEntity e = new TbPelangganEntity();
        assertEquals(Pelanggan.SUMBER_STAFF, e.getSumber());
        assertFalse(e.isPortalSourced());

        e.setSumber(null);
        e.normalizeContacts();
        assertEquals(Pelanggan.SUMBER_STAFF, e.getSumber());

        e.setSumber(Pelanggan.SUMBER_PORTAL);
        assertTrue(e.isPortalSourced());
    }

    @Test
    @DisplayName("round-trips provenance, merge pointer and an absent vehicle through the domain model")
    void domainRoundTrip() {
        TbPelangganEntity e = new TbPelangganEntity();
        e.setId(7L);
        e.setNamaPelanggan("Budi");
        e.setSumber(Pelanggan.SUMBER_PORTAL);
        e.setMergedIntoId(3L);

        Pelanggan domain = e.toDomain();
        assertNull(domain.getNopol());
        assertNull(domain.getMerk());
        assertEquals(Pelanggan.SUMBER_PORTAL, domain.getSumber());
        assertEquals(3L, domain.getMergedIntoId());

        TbPelangganEntity back = TbPelangganEntity.fromDomain(domain);
        assertEquals(Pelanggan.SUMBER_PORTAL, back.getSumber());
        assertEquals(3L, back.getMergedIntoId());
        assertNull(back.getNopol());
    }
}
