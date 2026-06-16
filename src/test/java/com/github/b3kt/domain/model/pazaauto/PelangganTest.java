package com.github.b3kt.domain.model.pazaauto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PelangganTest {

    @Test
    @DisplayName("Default constructor via NoArgsConstructor")
    void defaultConstructor() {
        Pelanggan p = new Pelanggan();
        assertNull(p.getId());
    }

    @Test
    @DisplayName("Parameterized constructor sets required fields")
    void parameterizedConstructor() {
        Pelanggan p = new Pelanggan("B1234XYZ", "Budi", "Toyota");
        assertEquals("B1234XYZ", p.getNopol());
        assertEquals("Budi", p.getNamaPelanggan());
        assertEquals("Toyota", p.getMerk());
    }

    @Test
    @DisplayName("Setters and getters")
    void settersAndGetters() {
        Pelanggan p = new Pelanggan();
        p.setId(1L);
        p.setNopol("B1234CD");
        p.setNamaPelanggan("Agus");
        p.setAlamat("Jl. Merdeka");
        p.setContactPerson("Agus");
        p.setTelepon("08123456789");
        p.setMerk("Honda");
        p.setJenis("Mobil");
        p.setEmail("agus@x.com");
        p.setJenisKelamin("L");
        p.setKeterangan("Reguler");
        p.setKodePos("12345");
        p.setKota("Jakarta");
        p.setNoHp("08123456789");
        p.setNoTelepon("0211234567");
        p.setTanggalJoin(LocalDate.of(2024, 1, 1));

        assertEquals(1L, p.getId());
        assertEquals("B1234CD", p.getNopol());
        assertEquals("Agus", p.getNamaPelanggan());
        assertEquals("Jl. Merdeka", p.getAlamat());
        assertEquals("Honda", p.getMerk());
        assertEquals("Mobil", p.getJenis());
    }

    @Test
    @DisplayName("setNopol rejects null")
    void setNopolNull() {
        Pelanggan p = new Pelanggan();
        assertThrows(IllegalArgumentException.class, () -> p.setNopol(null));
    }

    @Test
    @DisplayName("setNopol rejects blank")
    void setNopolBlank() {
        Pelanggan p = new Pelanggan();
        assertThrows(IllegalArgumentException.class, () -> p.setNopol("  "));
    }

    @Test
    @DisplayName("setNamaPelanggan rejects null")
    void setNamaPelangganNull() {
        Pelanggan p = new Pelanggan();
        assertThrows(IllegalArgumentException.class, () -> p.setNamaPelanggan(null));
    }

    @Test
    @DisplayName("setNamaPelanggan rejects blank")
    void setNamaPelangganBlank() {
        Pelanggan p = new Pelanggan();
        assertThrows(IllegalArgumentException.class, () -> p.setNamaPelanggan("  "));
    }

    @Test
    @DisplayName("setMerk rejects null")
    void setMerkNull() {
        Pelanggan p = new Pelanggan();
        assertThrows(IllegalArgumentException.class, () -> p.setMerk(null));
    }

    @Test
    @DisplayName("setMerk rejects blank")
    void setMerkBlank() {
        Pelanggan p = new Pelanggan();
        assertThrows(IllegalArgumentException.class, () -> p.setMerk("  "));
    }
}
