package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPenjualanDetailEntity;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbPenjualanDetailRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class TbPenjualanDetailServiceTest {

    @Mock
    TbPenjualanDetailRepository repository;

    @InjectMocks
    TbPenjualanDetailService service;

    @Test
    @DisplayName("getRepository returns injected repository")
    void getRepository() {
        assertSame(repository, service.getRepository());
    }

    @Test
    @DisplayName("setEntityId sets noPenjualan")
    void setEntityId() {
        TbPenjualanDetailEntity entity = new TbPenjualanDetailEntity();
        service.setEntityId(entity, 1L);
        assertEquals(1L, entity.getId());
    }
}
