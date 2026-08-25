package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbAbsensiConfigEntity;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbAbsensiConfigRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TbAbsensiConfigServiceTest {

    @Mock
    TbAbsensiConfigRepository repository;

    @Mock
    PanacheQuery<TbAbsensiConfigEntity> query;

    @InjectMocks
    TbAbsensiConfigService service;

    private TbAbsensiConfigEntity config;

    @BeforeEach
    void setUp() {
        config = new TbAbsensiConfigEntity();
        config.setId(1L);
        config.setConfigKey("shift.start");
        config.setConfigValue("08:00");
        config.setConfigType("TIME");
    }

    @Test
    @DisplayName("getStringConfig returns value when config exists")
    void testGetStringConfig_found() {
        when(repository.find(eq("configKey"), eq("shift.start"))).thenReturn(query);
        when(query.firstResult()).thenReturn(config);

        String result = service.getStringConfig("shift.start", "09:00");

        assertEquals("08:00", result);
    }

    @Test
    @DisplayName("getStringConfig returns default when config not found")
    void testGetStringConfig_notFound() {
        when(repository.find(eq("configKey"), eq("missing"))).thenReturn(query);
        when(query.firstResult()).thenReturn(null);

        String result = service.getStringConfig("missing", "defaultVal");

        assertEquals("defaultVal", result);
    }

    @Test
    @DisplayName("getIntegerConfig parses integer value")
    void testGetIntegerConfig_found() {
        config.setConfigValue("42");
        when(repository.find(eq("configKey"), eq("max.retries"))).thenReturn(query);
        when(query.firstResult()).thenReturn(config);

        int result = service.getIntegerConfig("max.retries", 5);

        assertEquals(42, result);
    }

    @Test
    @DisplayName("getIntegerConfig returns default on invalid value")
    void testGetIntegerConfig_invalid() {
        config.setConfigValue("abc");
        when(repository.find(eq("configKey"), eq("max.retries"))).thenReturn(query);
        when(query.firstResult()).thenReturn(config);

        int result = service.getIntegerConfig("max.retries", 5);

        assertEquals(5, result);
    }

    @Test
    @DisplayName("getIntegerConfig returns default when config not found")
    void testGetIntegerConfig_notFound() {
        when(repository.find(eq("configKey"), eq("missing"))).thenReturn(query);
        when(query.firstResult()).thenReturn(null);

        int result = service.getIntegerConfig("missing", 10);

        assertEquals(10, result);
    }

    @Test
    @DisplayName("getBooleanConfig parses true value")
    void testGetBooleanConfig_found_true() {
        config.setConfigValue("true");
        when(repository.find(eq("configKey"), eq("feature.enabled"))).thenReturn(query);
        when(query.firstResult()).thenReturn(config);

        boolean result = service.getBooleanConfig("feature.enabled", false);

        assertTrue(result);
    }

    @Test
    @DisplayName("getBooleanConfig parses false value")
    void testGetBooleanConfig_found_false() {
        config.setConfigValue("false");
        when(repository.find(eq("configKey"), eq("feature.enabled"))).thenReturn(query);
        when(query.firstResult()).thenReturn(config);

        boolean result = service.getBooleanConfig("feature.enabled", true);

        assertFalse(result);
    }

    @Test
    @DisplayName("getBooleanConfig returns default when config not found")
    void testGetBooleanConfig_notFound() {
        when(repository.find(eq("configKey"), eq("missing"))).thenReturn(query);
        when(query.firstResult()).thenReturn(null);

        boolean result = service.getBooleanConfig("missing", true);

        assertTrue(result);
    }

    @Test
    @DisplayName("findAll delegates to repository")
    void testFindAll() {
        when(repository.listAll()).thenReturn(List.of(config));
        assertEquals(1, service.findAll().size());
    }

    @Test
    @DisplayName("findById returns entity when found")
    void testFindById_found() {
        when(repository.findByIdOptional(1L)).thenReturn(Optional.of(config));
        assertNotNull(service.findById(1L));
    }

    @Test
    @DisplayName("findById throws when not found")
    void testFindById_notFound() {
        when(repository.findByIdOptional(999L)).thenReturn(Optional.empty());
        assertThrows(jakarta.persistence.EntityNotFoundException.class, () -> service.findById(999L));
    }

    @Test
    @DisplayName("create persists entity")
    void testCreate() {
        doNothing().when(repository).persist(any(TbAbsensiConfigEntity.class));
        assertNotNull(service.create(config));
    }

    @Test
    @DisplayName("update merges entity when found")
    void testUpdate_found() {
        when(repository.findByIdOptional(1L)).thenReturn(Optional.of(config));
        jakarta.persistence.EntityManager em = mock(jakarta.persistence.EntityManager.class);
        when(repository.getEntityManager()).thenReturn(em);
        when(em.merge(any(TbAbsensiConfigEntity.class))).thenReturn(config);
        assertNotNull(service.update(1L, config));
    }

    @Test
    @DisplayName("update throws when not found")
    void testUpdate_notFound() {
        when(repository.findByIdOptional(999L)).thenReturn(Optional.empty());
        assertThrows(jakarta.persistence.EntityNotFoundException.class, () -> service.update(999L, config));
    }

    @Test
    @DisplayName("delete calls deleteById")
    void testDelete() {
        when(repository.deleteById(1L)).thenReturn(true);
        service.delete(1L);
        verify(repository).deleteById(1L);
    }
}
