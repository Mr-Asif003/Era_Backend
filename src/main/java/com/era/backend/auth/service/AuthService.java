package com.era.backend.auth.service;

import com.era.backend.auth.dto.AuthResponse;
import com.era.backend.auth.dto.LoginRequest;
import com.era.backend.auth.dto.RegisterRequest;
import com.era.backend.auth.jwt.JwtUtil;
import com.era.backend.user.model.User;
import com.era.backend.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.NoSuchElementException;

/**
 * Handles register / login / refresh-token-rotation / logout.
 *
 * Refresh tokens are stored as Redis keys (refresh:{jti} -> userId) with a
 * TTL matching the token's expiry. On rotation, the old key is deleted and
 * a new pair is issued. On logout, the key is deleted immediately - this is
 * what makes logout instant rather than waiting for natural expiry.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    private static final String REFRESH_PREFIX = "refresh:";

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
                .isOnline(false)
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
        String redisKey = REFRESH_PREFIX + jti;
        String userId = redisTemplate.opsForValue().get(redisKey);

        if (userId == null) {
            // Token not found in Redis -> already used/rotated/revoked (replay attempt)
            throw new BadCredentialsException("Refresh token has been revoked");
        }

        // Rotation: invalidate the old token immediately
        redisTemplate.delete(redisKey);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        return issueTokenPair(user);
    }

    public void logout(String refreshToken) {
        if (jwtUtil.isTokenValid(refreshToken)) {
            Claims claims = jwtUtil.extractClaims(refreshToken);
            String jti = claims.getId();
            if (jti != null) {
                redisTemplate.delete(REFRESH_PREFIX + jti);
            }
        }
    }

    private AuthResponse issueTokenPair(User user) {
        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        Claims refreshClaims = jwtUtil.extractClaims(refreshToken);
        String jti = refreshClaims.getId();

        redisTemplate.opsForValue().set(
                REFRESH_PREFIX + jti,
                user.getId(),
                Duration.ofMillis(jwtUtil.getRefreshExpiryMillis())
        );

        return AuthResponse.builder()
                .user(user)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}
