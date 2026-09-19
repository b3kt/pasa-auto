package com.github.b3kt.application.service.impl;

import com.github.b3kt.application.dto.LoginResponse;
import com.github.b3kt.application.dto.UserInfo;
import com.github.b3kt.application.mapper.UserMapper;
import com.github.b3kt.application.service.AuthService;
import com.github.b3kt.application.service.RefreshTokenService;
import com.github.b3kt.domain.exception.AuthenticationException;
import com.github.b3kt.domain.model.User;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbKaryawanRepository;
import com.github.b3kt.infrastructure.repository.UserRepository;
import com.github.b3kt.infrastructure.security.JwtTokenService;
import com.github.b3kt.infrastructure.security.PasswordEncoder;
import com.github.b3kt.infrastructure.security.PasswordPolicy;
import com.github.b3kt.infrastructure.security.RefreshTokenClaims;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * Implementation of authentication service.
 * This orchestrates the authentication use cases.
 */
@ApplicationScoped
public class AuthServiceImpl implements AuthService {

    @Inject
    UserRepository userRepository;

    @Inject
    TbKaryawanRepository tbKaryawanRepository;

    @Inject
    JwtTokenService jwtTokenService;

    @Inject
    PasswordEncoder passwordEncoder;

    @Inject
    RefreshTokenService refreshTokenService;

    @Override
    public LoginResponse login(String username, String password) {
        // Find user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AuthenticationException("Invalid username or password"));

        // Check if user can authenticate
        if (!user.canAuthenticate()) {
            throw new AuthenticationException("User account is not active");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new AuthenticationException("Invalid username or password");
        }

        // Get related karyawan info
        tbKaryawanRepository.findByUsername(username)
                .ifPresent(karyawan -> {
                    user.setKaryawanId(karyawan.getId());
                    user.setKaryawanNama(karyawan.getNamaKaryawan());
                });

        return issueTokens(user, refreshTokenService.issue(user));
    }

    @Override
    public UserInfo getUserInfo(JsonWebToken jwt) {
        return jwtTokenService.extractUserInfo(jwt);
    }
    
    @Override
    public LoginResponse refreshToken(String refreshToken) {
        // Validate the refresh token, then mark it used (rejects rotated or revoked tokens)
        RefreshTokenClaims claims = jwtTokenService.validateRefreshToken(refreshToken);
        if (claims == null) {
            throw new AuthenticationException("Invalid or expired refresh token");
        }
        String familyId = refreshTokenService.consume(claims);
        String username = claims.username();
        
        // Find user by username
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AuthenticationException("User not found"));
        
        // Check if user can still authenticate
        if (!user.canAuthenticate()) {
            refreshTokenService.revokeAllForUser(username);
            throw new AuthenticationException("User account is not active");
        }
        
        // Get related karyawan info
        tbKaryawanRepository.findByUsername(username)
                .ifPresent(karyawan -> {
                    user.setKaryawanId(karyawan.getId());
                    user.setKaryawanNama(karyawan.getNamaKaryawan());
                });
        
        return issueTokens(user, refreshTokenService.issue(user, familyId));
    }

    @Override
    public void logout(String username, String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            refreshTokenService.revokeAllForUser(username);
            return;
        }
        RefreshTokenClaims claims = jwtTokenService.validateRefreshToken(refreshToken);
        if (claims != null && claims.username().equals(username)) {
            refreshTokenService.revokeSession(claims);
        }
    }

    @Override
    public LoginResponse changePassword(String username, String currentPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                .filter(User::canAuthenticate)
                .orElseThrow(() -> new AuthenticationException("User account is not active"));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new AuthenticationException("Current password is incorrect");
        }
        PasswordPolicy.validate(newPassword, username);
        if (newPassword.equals(currentPassword)) {
            throw new IllegalArgumentException("New password must be different from the current password");
        }

        user.setMustChangePassword(false);
        userRepository.updatePassword(username, passwordEncoder.encode(newPassword), false);

        // Sessions opened with the old password (e.g. on another device) end here; this one continues with new tokens
        refreshTokenService.revokeAllForUser(username);
        tbKaryawanRepository.findByUsername(username)
                .ifPresent(karyawan -> {
                    user.setKaryawanId(karyawan.getId());
                    user.setKaryawanNama(karyawan.getNamaKaryawan());
                });
        return issueTokens(user, refreshTokenService.issue(user));
    }

    private LoginResponse issueTokens(User user, String refreshToken) {
        UserInfo userInfo = UserMapper.toUserInfo(user);
        LoginResponse response = new LoginResponse(
                jwtTokenService.generateToken(user),
                refreshToken,
                userInfo.getUsername(),
                userInfo.getEmail(),
                jwtTokenService.getTokenExpirationSeconds());
        response.setMustChangePassword(user.isMustChangePassword());
        return response;
    }
}
