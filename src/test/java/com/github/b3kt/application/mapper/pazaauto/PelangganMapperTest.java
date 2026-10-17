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
    @DisplayName("toDto exposes telepon as an alias of noTelepon, ignoring the legacy tlpn value")
    void teleponAlias() {
        TbPelangganEntity e = new TbPelangganEntity();
        e.setNamaPelanggan("Budi");
        e.setTelepon("legacy");
        e.setNoHp("0812");
        e.setNoTelepon("0215551234");
        e.setSumber(Pelanggan.SUMBER_PORTAL);

        PelangganDto dto = mapper.toDto(e);

        assertEquals("0215551234", dto.getTelepon());
        assertEquals("0215551234", dto.getNoTelepon());
        assertEquals("PORTAL", dto.getSumber());
    }

    @Test
    @DisplayName("toDto leaves telepon empty when there is no landline (no copy of the mobile number)")
    void teleponWithoutLandline() {
        TbPelangganEntity e = new TbPelangganEntity();
        e.setNamaPelanggan("Budi");
        e.setNoHp("0812");

        assertNull(mapper.toDto(e).getTelepon());
    }

    @Test
    @DisplayName("toEntity never takes sumber from the request body")
    void sumberIsServerOwned() {
        PelangganDto dto = PelangganDto.builder().namaPelanggan("Budi").sumber("PORTAL").build();

        assertEquals(Pelanggan.SUMBER_STAFF, mapper.toEntity(dto).getSumber());
    }
}
