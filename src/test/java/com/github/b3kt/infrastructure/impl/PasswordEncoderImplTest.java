package com.github.b3kt.infrastructure.impl;

import com.github.b3kt.infrastructure.security.impl.PasswordEncoderImpl;
import lombok.extern.slf4j.Slf4j;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import com.github.b3kt.infrastructure.security.PasswordEncoder;

@Slf4j
public class PasswordEncoderImplTest {

    PasswordEncoder passwordEncoder = new PasswordEncoderImpl();

    @Test
    void testEncode() {
        String rawPassword = "password";
        String encodedPassword = passwordEncoder.encode(rawPassword);
        assertTrue(passwordEncoder.matches(rawPassword, encodedPassword));
    }

    @Test
    void testMatches() {
        String rawPassword = "password";
        String encodedPassword = "$2a$12$g5vqE9aRiQa64ZQy9.juIOPk/6l7aFFcpQSVmTN8hG2yv54bhbRWW";
        assertTrue(passwordEncoder.matches(rawPassword, encodedPassword));
    }
    
}
