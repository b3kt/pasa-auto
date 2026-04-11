package com.github.b3kt.infrastructure.security.impl;

import com.github.b3kt.infrastructure.security.PasswordEncoder;

import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PasswordEncoderImpl implements PasswordEncoder {

    private static final String BCRYPT_PREFIX = "$2";

    @Override
    public String encode(String rawPassword) {
        return BcryptUtil.bcryptHash(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        
        if (encodedPassword.startsWith(BCRYPT_PREFIX)) {
            return BcryptUtil.matches(rawPassword, encodedPassword);
        }
        
        return rawPassword.equals(encodedPassword);
    }

    public boolean isBcryptHash(String encodedPassword) {
        return encodedPassword != null && encodedPassword.startsWith(BCRYPT_PREFIX);
    }
}

