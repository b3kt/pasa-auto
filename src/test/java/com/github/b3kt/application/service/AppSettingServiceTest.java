package com.github.b3kt.application.service;

import com.github.b3kt.infrastructure.persistence.entity.AppSettingEntity;
import com.github.b3kt.infrastructure.persistence.repository.AppSettingRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AppSettingService Tests")
class AppSettingServiceTest {

    @Mock
    AppSettingRepository repository;

    @Mock
    PanacheQuery<AppSettingEntity> query;

    @InjectMocks
    AppSettingService service;

    private AppSettingEntity stubConfig(String value) {
        AppSettingEntity config = new AppSettingEntity();
        config.setConfigKey("customer.access.enabled");
        config.setConfigValue(value);
        when(repository.find(eq("configKey"), eq("customer.access.enabled"))).thenReturn(query);
        when(query.firstResult()).thenReturn(config);
        return config;
    }

    @Test
    @DisplayName("getRepository exposes the injected repository to the CRUD base")
    void getRepository() {
        assertSame(repository, service.getRepository());
    }

    @Test
    @DisplayName("setEntityId assigns the id")
    void setEntityId() {
        AppSettingEntity entity = new AppSettingEntity();

        service.setEntityId(entity, 9L);

        assertEquals(9L, entity.getId());
    }

    @Nested
    @DisplayName("getStringConfig")
    class GetStringConfigTests {

        @Test
        @DisplayName("returns the stored value")
        void stored() {
            stubConfig("true");

            assertEquals("true", service.getStringConfig("customer.access.enabled", "false"));
        }

        @Test
        @DisplayName("returns the default when the key is absent")
        void absent() {
            when(repository.find(eq("configKey"), eq("missing"))).thenReturn(query);
            when(query.firstResult()).thenReturn(null);

            assertEquals("fallback", service.getStringConfig("missing", "fallback"));
        }
    }

    @Nested
    @DisplayName("getBooleanConfig")
    class GetBooleanConfigTests {

        @Test
        @DisplayName("parses a true value")
        void trueValue() {
            stubConfig("true");

            assertTrue(service.getBooleanConfig("customer.access.enabled", false));
        }

        @Test
        @DisplayName("parses a false value")
        void falseValue() {
            stubConfig("false");

            assertFalse(service.getBooleanConfig("customer.access.enabled", true));
        }

        @Test
        @DisplayName("falls back to the supplied default when the key is absent")
        void absent() {
            when(repository.find(eq("configKey"), eq("missing"))).thenReturn(query);
            when(query.firstResult()).thenReturn(null);

            assertTrue(service.getBooleanConfig("missing", true));
            assertFalse(service.getBooleanConfig("missing", false));
        }
    }

    @Nested
    @DisplayName("setBooleanConfig")
    class SetBooleanConfigTests {

        @Test
        @DisplayName("persists a new row on first write")
        void creates() {
            when(repository.find(eq("configKey"), eq("new.key"))).thenReturn(query);
            when(query.firstResult()).thenReturn(null);

            service.setBooleanConfig("new.key", true, "a flag", "admin");

            ArgumentCaptor<AppSettingEntity> captor = ArgumentCaptor.forClass(AppSettingEntity.class);
            verify(repository).persist(captor.capture());
            AppSettingEntity saved = captor.getValue();
            assertEquals("new.key", saved.getConfigKey());
            assertEquals("BOOLEAN", saved.getConfigType());
            assertEquals("a flag", saved.getDescription());
            assertEquals("true", saved.getConfigValue());
            assertEquals("admin", saved.getUpdatedBy());
            assertNotNull(saved.getUpdatedAt());
        }

        @Test
        @DisplayName("updates an existing row without inserting a second one")
        void updates() {
            AppSettingEntity existing = new AppSettingEntity();
            existing.setId(3L);
            existing.setConfigKey("customer.access.enabled");
            when(repository.find(eq("configKey"), eq("customer.access.enabled"))).thenReturn(query);
            when(query.firstResult()).thenReturn(existing);

            service.setBooleanConfig("customer.access.enabled", false, "toggle", "owner");

            assertEquals("false", existing.getConfigValue());
            verify(repository, never()).persist(any(AppSettingEntity.class));
        }
    }
}
