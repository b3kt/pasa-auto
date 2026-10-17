package com.github.b3kt.application.mapper.pazaauto;

import com.github.b3kt.application.dto.pazaauto.PelangganDto;
import com.github.b3kt.domain.model.pazaauto.Pelanggan;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPelangganEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PelangganMapperTest {

    private final PelangganMapper mapper = PelangganMapper.INSTANCE;

    @Test
    @DisplayName("toDto never returns the deprecated landline, and carries the contact-source flag")
    void landlineNeverReturned() {
        TbPelangganEntity e = new TbPelangganEntity();
        e.setNamaPelanggan("Budi");
        e.setTelepon("legacy");
        e.setNoHp("0812");
        e.setNoTelepon("0215551234");
        e.setSumber(Pelanggan.SUMBER_PORTAL);
        e.setContactFromAccount(true);

        PelangganDto dto = mapper.toDto(e);

        assertNull(dto.getTelepon());
        assertNull(dto.getNoTelepon());
        assertEquals("0812", dto.getNoHp());
        assertEquals("PORTAL", dto.getSumber());
        assertTrue(dto.isContactFromAccount());
    }

    @Test
    @DisplayName("toEntity ignores the deprecated landline")
    void landlineIgnoredOnWrite() {
        PelangganDto dto = PelangganDto.builder().namaPelanggan("Budi").telepon("021").noTelepon("022").build();

        TbPelangganEntity e = mapper.toEntity(dto);

        assertNull(e.getTelepon());
        assertNull(e.getNoTelepon());
    }

    @Test
    @DisplayName("toEntity never takes sumber from the request body")
    void sumberIsServerOwned() {
        PelangganDto dto = PelangganDto.builder().namaPelanggan("Budi").sumber("PORTAL").build();

        assertEquals(Pelanggan.SUMBER_STAFF, mapper.toEntity(dto).getSumber());
    }
}
