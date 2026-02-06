package com.github.b3kt.infrastructure.security;

import com.github.b3kt.domain.model.User;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@QuarkusTest
public class JwtTokenServiceImplTest {

    @Inject
    JwtTokenServiceImpl jwtTokenService;

    @Test
    public void generateToken_shouldNotThrowNPE_whenKaryawanIdIsNull() {
        // Arrange
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setRoles(new HashSet<>());
        // karyawanId and karyawanNama are null by default

        // Act & Assert
        assertDoesNotThrow(() -> {
            String token = jwtTokenService.generateToken(user);
            assertNotNull(token);
        });
    }

    @Disabled
    @Test
    public void generateToken_shouldWork_whenKaryawanIdIsPresent() {
        // Arrange
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setRoles(new HashSet<>());
        user.setKaryawanId(123L);
        user.setKaryawanNama("Test Karyawan");

        // Act & Assert
        assertDoesNotThrow(() -> {
            String token = jwtTokenService.generateToken(user);
            assertNotNull(token);
        });
    }
}
