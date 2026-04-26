package com.github.b3kt.infrastructure.persistence.listener;

import com.github.b3kt.infrastructure.persistence.entity.BaseEntity;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class AuditListenerTest {

    @InjectMock
    SecurityIdentity securityIdentity;

    @Inject
    AuditListener auditListener;

    private TestBaseEntity testEntity;

    @BeforeEach
    void setUp() {
        testEntity = new TestBaseEntity();
    }

    @Test
    @DisplayName("Should set audit fields on prePersist for BaseEntity")
    void testPrePersistWithBaseEntity() {
        when(securityIdentity.getPrincipal()).thenReturn(mock(org.eclipse.microprofile.jwt.JsonWebToken.class));
        when(securityIdentity.getPrincipal().getName()).thenReturn("testuser");
        when(securityIdentity.isAnonymous()).thenReturn(false);

        LocalDateTime before = LocalDateTime.now().minusSeconds(1);

        auditListener.prePersist(testEntity);

        assertNotNull(testEntity.getCreatedAt());
        assertNotNull(testEntity.getUpdatedAt());
        assertEquals("testuser", testEntity.getCreatedBy());
        assertEquals("testuser", testEntity.getUpdatedBy());
        assertEquals(Integer.valueOf(0), testEntity.getVersion());
        
        assertTrue(testEntity.getCreatedAt().isAfter(before));
        assertTrue(testEntity.getUpdatedAt().isAfter(before));
        
        verify(securityIdentity, times(3)).getPrincipal();
        verify(securityIdentity).isAnonymous();
    }

    @Test
    @DisplayName("Should not override version if already set on prePersist")
    void testPrePersistWithExistingVersion() {
        testEntity.setVersion(5);
        when(securityIdentity.getPrincipal()).thenReturn(mock(org.eclipse.microprofile.jwt.JsonWebToken.class));
        when(securityIdentity.getPrincipal().getName()).thenReturn("testuser");
        when(securityIdentity.isAnonymous()).thenReturn(false);

        auditListener.prePersist(testEntity);

        assertEquals(Integer.valueOf(5), testEntity.getVersion());
    }

    @Test
    @DisplayName("Should set system as user when security identity is null")
    void testPrePersistWithNullSecurityIdentity() {
        // Given - create new auditListener with null security identity
        AuditListener listener = new AuditListener(null);

        listener.prePersist(testEntity);

        assertNotNull(testEntity.getCreatedAt());
        assertNotNull(testEntity.getUpdatedAt());
        assertEquals("system", testEntity.getCreatedBy());
        assertEquals("system", testEntity.getUpdatedBy());
    }

    @Test
    @DisplayName("Should set system as user when security identity is anonymous")
    void testPrePersistWithAnonymousUser() {
        when(securityIdentity.isAnonymous()).thenReturn(true);

        auditListener.prePersist(testEntity);

        assertNotNull(testEntity.getCreatedAt());
        assertNotNull(testEntity.getUpdatedAt());
        assertEquals("system", testEntity.getCreatedBy());
        assertEquals("system", testEntity.getUpdatedBy());
        
        verify(securityIdentity).getPrincipal();
        verifyNoMoreInteractions(securityIdentity);
    }

    @Test
    @DisplayName("Should update audit fields on preUpdate for BaseEntity")
    void testPreUpdateWithBaseEntity() {
        testEntity.setCreatedAt(LocalDateTime.now().minusDays(1));
        testEntity.setCreatedBy("originaluser");
        when(securityIdentity.getPrincipal()).thenReturn(mock(org.eclipse.microprofile.jwt.JsonWebToken.class));
        when(securityIdentity.getPrincipal().getName()).thenReturn("updateuser");
        when(securityIdentity.isAnonymous()).thenReturn(false);

        LocalDateTime before = LocalDateTime.now().minusSeconds(1);

        auditListener.preUpdate(testEntity);

        assertNotNull(testEntity.getUpdatedAt());
        assertEquals("updateuser", testEntity.getUpdatedBy());
        assertEquals("originaluser", testEntity.getCreatedBy());
        assertTrue(testEntity.getUpdatedAt().isAfter(before));
        
        verify(securityIdentity, times(3)).getPrincipal();
        verify(securityIdentity).isAnonymous();
    }

    @Test
    @DisplayName("Should set system as updater when security identity is null on update")
    void testPreUpdateWithNullSecurityIdentity() {
        AuditListener listener = new AuditListener(null);

        listener.preUpdate(testEntity);

        assertNotNull(testEntity.getUpdatedAt());
        assertEquals("system", testEntity.getUpdatedBy());
    }

    @Test
    @DisplayName("Should set system as updater when security identity is anonymous on update")
    void testPreUpdateWithAnonymousUser() {
        when(securityIdentity.isAnonymous()).thenReturn(true);

        auditListener.preUpdate(testEntity);

        assertNotNull(testEntity.getUpdatedAt());
        assertEquals("system", testEntity.getUpdatedBy());
        
        verify(securityIdentity).getPrincipal();
        verifyNoMoreInteractions(securityIdentity);
    }

    @Test
    @DisplayName("Should ignore non-BaseEntity on prePersist")
    void testPrePersistWithNonBaseEntity() {
        Object nonBaseEntity = new Object();

        assertDoesNotThrow(() -> auditListener.prePersist(nonBaseEntity));
    }

    @Test
    @DisplayName("Should ignore non-BaseEntity on preUpdate")
    void testPreUpdateWithNonBaseEntity() {
        Object nonBaseEntity = new Object();

        assertDoesNotThrow(() -> auditListener.preUpdate(nonBaseEntity));
    }

    @Test
    @DisplayName("Should handle null principal gracefully")
    void testPrePersistWithNullPrincipal() {
        when(securityIdentity.getPrincipal()).thenReturn(null);
        when(securityIdentity.isAnonymous()).thenReturn(false);

        assertDoesNotThrow(() -> auditListener.prePersist(testEntity));

        assertEquals("system", testEntity.getCreatedBy());
        assertEquals("system", testEntity.getUpdatedBy());
    }

    @Test
    @DisplayName("Should handle principal name null gracefully")
    void testPrePersistWithNullPrincipalName() {
        org.eclipse.microprofile.jwt.JsonWebToken principal = mock(org.eclipse.microprofile.jwt.JsonWebToken.class);
        when(securityIdentity.getPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn(null);
        when(securityIdentity.isAnonymous()).thenReturn(false);

        assertDoesNotThrow(() -> auditListener.prePersist(testEntity));

        assertNull(testEntity.getCreatedBy());
        assertNull(testEntity.getUpdatedBy());
    }

    private static class TestBaseEntity extends BaseEntity {
    }
}