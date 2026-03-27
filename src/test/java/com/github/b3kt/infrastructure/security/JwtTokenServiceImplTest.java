package com.github.b3kt.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenServiceImplTest {

    @Test
    @DisplayName("Should validate refresh token format")
    void validateRefreshToken_shouldCheckFormat() {
        JwtTokenServiceImpl service = new JwtTokenServiceImpl();
        
        assertNull(service.validateRefreshToken(null));
        assertNull(service.validateRefreshToken(""));
        assertNull(service.validateRefreshToken("invalid"));
        assertNull(service.validateRefreshToken("only.two"));
        assertNull(service.validateRefreshToken("not.a.valid.jwt.token"));
    }

    @Test
    @DisplayName("Should return expiration seconds greater than zero")
    void getTokenExpirationSeconds_shouldReturnConfiguredValue() {
        JwtTokenServiceImpl service = new JwtTokenServiceImpl();
        service.setExpirationHours(24);
        
        long result = service.getTokenExpirationSeconds();
        
        assertEquals(86400L, result);
    }

    @Test
    @DisplayName("Should decode valid refresh token payload")
    void validateRefreshToken_shouldDecodePayload() {
        JwtTokenServiceImpl service = new JwtTokenServiceImpl();
        
        String validPayload = "{\"sub\":\"testuser\",\"type\":\"refresh\",\"exp\":9999999999}";
        String encoded = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(validPayload.getBytes());
        String token = "header." + encoded + ".signature";
        
        String result = service.validateRefreshToken(token);
        
        assertEquals("testuser", result);
    }

    @Test
    @DisplayName("Should reject token without refresh type")
    void validateRefreshToken_shouldRejectNonRefreshToken() {
        JwtTokenServiceImpl service = new JwtTokenServiceImpl();
        
        String payload = "{\"sub\":\"testuser\",\"exp\":9999999999}";
        String encoded = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes());
        String token = "header." + encoded + ".signature";
        
        assertNull(service.validateRefreshToken(token));
    }

    @Test
    @DisplayName("Should reject expired token")
    void validateRefreshToken_shouldRejectExpiredToken() {
        JwtTokenServiceImpl service = new JwtTokenServiceImpl();
        
        String payload = "{\"sub\":\"testuser\",\"type\":\"refresh\",\"exp\":0}";
        String encoded = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes());
        String token = "header." + encoded + ".signature";
        
        assertNull(service.validateRefreshToken(token));
    }

    @Test
    @DisplayName("Should handle malformed Base64 gracefully")
    void validateRefreshToken_shouldHandleMalformedBase64() {
        JwtTokenServiceImpl service = new JwtTokenServiceImpl();
        
        assertNull(service.validateRefreshToken("not!!!base64@!.header.signature"));
    }
}
