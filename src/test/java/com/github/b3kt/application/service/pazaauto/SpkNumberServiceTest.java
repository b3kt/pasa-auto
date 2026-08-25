package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSpkEntity;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbSpkRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Parameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpkNumberServiceTest {

    @Mock
    TbSpkRepository repository;

    @Mock
    PanacheQuery<TbSpkEntity> query;

    @InjectMocks
    SpkNumberService service;

    private TbSpkEntity spk;

    @BeforeEach
    void setUp() {
        spk = new TbSpkEntity();
        spk.setNoSpk("SPK0105");
    }

    @Test
    @DisplayName("getNextSpkNumber returns last matching SPK")
    void testGetNextSpkNumber_found() {
        when(repository.find(anyString(), any(Parameters.class))).thenReturn(query);
        when(query.list()).thenReturn(List.of(spk));

        String result = service.getNextSpkNumber("SPK01");

        assertEquals("SPK0105", result);
    }

    @Test
    @DisplayName("getNextSpkNumber returns default when none found")
    void testGetNextSpkNumber_notFound() {
        when(repository.find(anyString(), any(Parameters.class))).thenReturn(query);
        when(query.list()).thenReturn(List.of());

        String result = service.getNextSpkNumber("SPK0105");

        assertEquals("SPK010500", result);
    }

    @Test
    @DisplayName("generateNextSpkNumber increments last SPK number")
    void testGenerateNextSpkNumber_increments() {
        when(repository.find(anyString(), any(Parameters.class))).thenReturn(query);
        when(query.list()).thenReturn(List.of(spk));

        String result = service.generateNextSpkNumber("SPK01");

        assertEquals("SPK0106", result);
    }

    @Test
    @DisplayName("generateNextSpkNumber appends 01 when no existing SPK")
    void testGenerateNextSpkNumber_zero() {
        when(repository.find(anyString(), any(Parameters.class))).thenReturn(query);
        when(query.list()).thenReturn(List.of());

        String result = service.generateNextSpkNumber("SPK0105");

        assertEquals("SPK010501", result);
    }
}
