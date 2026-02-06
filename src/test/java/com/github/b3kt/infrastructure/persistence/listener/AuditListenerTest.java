package com.github.b3kt.infrastructure.persistence.listener;

import com.github.b3kt.infrastructure.persistence.entity.BaseEntity;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
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

    private AuditListener auditListener;
    private TestBaseEntity testEntity;

    @BeforeEach
    void setUp() {
        auditListener = new AuditListener(securityIdentity);
        testEntity = new TestBaseEntity();
    }

    @Test
    @DisplayName("Should set audit fields on prePersist for BaseEntity")
    void testPrePersistWithBaseEntity() {
        // Given
        when(securityIdentity.getPrincipal()).thenReturn(mock(org.eclipse.microprofile.jwt.JsonWebToken.class));
        when(securityIdentity.getPrincipal().getName()).thenReturn("testuser");
        when(securityIdentity.isAnonymous()).thenReturn(false);

        LocalDateTime before = LocalDateTime.now().minusSeconds(1);

        // When
        auditListener.prePersist(testEntity);

        // Then
        assertNotNull(testEntity.getCreatedAt());
        assertNotNull(testEntity.getUpdatedAt());
        assertEquals("testuser", testEntity.getCreatedBy());
        assertEquals("testuser", testEntity.getUpdatedBy());
        assertEquals(Integer.valueOf(0), testEntity.getVersion());
        
        // Verify timestamps are recent
        assertTrue(testEntity.getCreatedAt().isAfter(before));
        assertTrue(testEntity.getUpdatedAt().isAfter(before));
        
        verify(securityIdentity, times(3)).getPrincipal();
        verify(securityIdentity).isAnonymous();
    }

    @Test
    @DisplayName("Should not override version if already set on prePersist")
    void testPrePersistWithExistingVersion() {
        // Given
        testEntity.setVersion(5);
        when(securityIdentity.getPrincipal()).thenReturn(mock(org.eclipse.microprofile.jwt.JsonWebToken.class));
        when(securityIdentity.getPrincipal().getName()).thenReturn("testuser");
        when(securityIdentity.isAnonymous()).thenReturn(false);

        // When
        auditListener.prePersist(testEntity);

        // Then
        assertEquals(Integer.valueOf(5), testEntity.getVersion());
    }

    @Test
    @DisplayName("Should set system as user when security identity is null")
    void testPrePersistWithNullSecurityIdentity() {
        // Given
        auditListener = new AuditListener(null);

        // When
        auditListener.prePersist(testEntity);

        // Then
        assertNotNull(testEntity.getCreatedAt());
        assertNotNull(testEntity.getUpdatedAt());
        assertEquals("system", testEntity.getCreatedBy());
        assertEquals("system", testEntity.getUpdatedBy());
    }

    @Test
    @DisplayName("Should set system as user when security identity is anonymous")
    void testPrePersistWithAnonymousUser() {
        // Given
        when(securityIdentity.isAnonymous()).thenReturn(true);

        // When
        auditListener.prePersist(testEntity);

        // Then
        assertNotNull(testEntity.getCreatedAt());
        assertNotNull(testEntity.getUpdatedAt());
        assertEquals("system", testEntity.getCreatedBy());
        assertEquals("system", testEntity.getUpdatedBy());
        
//        verify(securityIdentity).isAnonymous();
        verify(securityIdentity).getPrincipal();
        verifyNoMoreInteractions(securityIdentity);
    }

    @Test
    @DisplayName("Should update audit fields on preUpdate for BaseEntity")
    void testPreUpdateWithBaseEntity() {
        // Given
        testEntity.setCreatedAt(LocalDateTime.now().minusDays(1));
        testEntity.setCreatedBy("originaluser");
        when(securityIdentity.getPrincipal()).thenReturn(mock(org.eclipse.microprofile.jwt.JsonWebToken.class));
        when(securityIdentity.getPrincipal().getName()).thenReturn("updateuser");
        when(securityIdentity.isAnonymous()).thenReturn(false);

        LocalDateTime before = LocalDateTime.now().minusSeconds(1);

        // When
        auditListener.preUpdate(testEntity);

        // Then
        assertNotNull(testEntity.getUpdatedAt());
        assertEquals("updateuser", testEntity.getUpdatedBy());
        
        // Verify original created fields are not changed
        assertEquals("originaluser", testEntity.getCreatedBy());
        
        // Verify timestamp is recent
        assertTrue(testEntity.getUpdatedAt().isAfter(before));
        
        verify(securityIdentity, times(3)).getPrincipal();
        verify(securityIdentity).isAnonymous();
    }

    @Test
    @DisplayName("Should set system as updater when security identity is null on update")
    void testPreUpdateWithNullSecurityIdentity() {
        // Given
        auditListener = new AuditListener(null);

        // When
        auditListener.preUpdate(testEntity);

        // Then
        assertNotNull(testEntity.getUpdatedAt());
        assertEquals("system", testEntity.getUpdatedBy());
    }

    @Test
    @DisplayName("Should set system as updater when security identity is anonymous on update")
    void testPreUpdateWithAnonymousUser() {
        // Given
        when(securityIdentity.isAnonymous()).thenReturn(true);

        // When
        auditListener.preUpdate(testEntity);

        // Then
        assertNotNull(testEntity.getUpdatedAt());
        assertEquals("system", testEntity.getUpdatedBy());
        
//        verify(securityIdentity).isAnonymous();
        verify(securityIdentity).getPrincipal();
        verifyNoMoreInteractions(securityIdentity);
    }

    @Test
    @DisplayName("Should ignore non-BaseEntity on prePersist")
    void testPrePersistWithNonBaseEntity() {
        // Given
        Object nonBaseEntity = new Object();

        // When
        assertDoesNotThrow(() -> auditListener.prePersist(nonBaseEntity));
    }

    @Test
    @DisplayName("Should ignore non-BaseEntity on preUpdate")
    void testPreUpdateWithNonBaseEntity() {
        // Given
        Object nonBaseEntity = new Object();

        // When
        assertDoesNotThrow(() -> auditListener.preUpdate(nonBaseEntity));
    }

    @Test
    @DisplayName("Should handle null principal gracefully")
    void testPrePersistWithNullPrincipal() {
        // Given
        when(securityIdentity.getPrincipal()).thenReturn(null);
        when(securityIdentity.isAnonymous()).thenReturn(false);

        // When
        assertDoesNotThrow(() -> auditListener.prePersist(testEntity));

        // Then
        assertEquals("system", testEntity.getCreatedBy());
        assertEquals("system", testEntity.getUpdatedBy());
    }

    @Test
    @DisplayName("Should handle principal name null gracefully")
    void testPrePersistWithNullPrincipalName() {
        // Given
        org.eclipse.microprofile.jwt.JsonWebToken principal = mock(org.eclipse.microprofile.jwt.JsonWebToken.class);
        when(securityIdentity.getPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn(null);
        when(securityIdentity.isAnonymous()).thenReturn(false);

        // When
        assertDoesNotThrow(() -> auditListener.prePersist(testEntity));

        // Then
        assertNull(testEntity.getCreatedBy());
        assertNull(testEntity.getUpdatedBy());
    }

    // Test implementation of BaseEntity
    private static class TestBaseEntity extends BaseEntity {
        // Implementation inherits all BaseEntity methods
    }
}
