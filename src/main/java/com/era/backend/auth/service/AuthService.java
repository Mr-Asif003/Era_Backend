package com.era.backend.auth.service;

import com.era.backend.auth.dto.AuthResponse;
import com.era.backend.auth.dto.LoginRequest;
import com.era.backend.auth.dto.RegisterRequest;
import com.era.backend.auth.jwt.JwtUtil;
import com.era.backend.auth.jwt.RefreshTokenStore;
import com.era.backend.user.model.User;
import com.era.backend.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.NoSuchElementException;

/**
 * Handles register / login / refresh-token-rotation / logout.
 *
 * Refresh tokens are tracked in {@link RefreshTokenStore} (jti -> userId)
 * with a TTL matching the token's expiry. On rotation, the old entry is
 * removed and a new pair is issued. On logout, the entry is removed
 * immediately - this is what makes logout instant rather than waiting for
 * natural expiry.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenStore refreshTokenStore;

    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new IllegalArgumentException("Username already taken");
        }

        User user = User.builder()
                .username(req.getUsername())
                .email(req.getEmail())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .displayName(req.getDisplayName())
                .number(req.getNumber())
                .avatarColor(randomAvatarColor())
                .online(false)
                // No email delivery service is wired up yet, so accounts are
                // considered verified immediately. Swap to `false` + wire a
                // real /auth/verify flow once email sending is implemented.
                .verified(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .eraVoice("default")
                .eraLanguage("en")
                .build();

        user = userRepository.save(user);
        return issueTokenPair(user);
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        return issueTokenPair(user);
    }

    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtUtil.isTokenValid(refreshToken)) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }

        Claims claims = jwtUtil.extractClaims(refreshToken);
        if (!"refresh".equals(claims.get("type"))) {
            throw new BadCredentialsException("Token is not a refresh token");
        }

        String jti = claims.getId();

        // Rotation: consuming removes it immediately, so a replayed/stolen
        // token can never be used twice.
        String userId = refreshTokenStore.consume(jti)
                .orElseThrow(() -> new BadCredentialsException("Refresh token has been revoked"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        return issueTokenPair(user);
    }

    public void logout(String refreshToken) {
        if (jwtUtil.isTokenValid(refreshToken)) {
            Claims claims = jwtUtil.extractClaims(refreshToken);
            String jti = claims.getId();
            if (jti != null) {
                refreshTokenStore.revoke(jti);
            }
        }
    }

    private AuthResponse issueTokenPair(User user) {
        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        Claims refreshClaims = jwtUtil.extractClaims(refreshToken);
        String jti = refreshClaims.getId();

        refreshTokenStore.store(jti, user.getId(), jwtUtil.getRefreshExpiryMillis());

        return AuthResponse.builder()
                .user(user)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    private static final String[] AVATAR_COLORS = {
            "#6366F1", "#EC4899", "#10B981", "#F59E0B", "#0EA5E9", "#8B5CF6"
    };

    private String randomAvatarColor() {
        return AVATAR_COLORS[(int) (Math.random() * AVATAR_COLORS.length)];
    }
}
