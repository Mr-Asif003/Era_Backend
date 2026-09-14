package com.era.backend.auth.jwt;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks issued refresh tokens (by JWT id / "jti") so they can be rotated
 * and revoked server-side, the same way a Redis-backed store would.
 *
 * This is an in-memory implementation: it keeps the backend to a single
 * process/dependency (just MongoDB), which is the right trade-off for this
 * app's current scale. If the backend is ever scaled to multiple instances,
 * swap this class's internals for a shared store (Redis, a Mongo
 * collection with a TTL index, etc) - callers don't need to change.
 */
@Component
public class RefreshTokenStore {

    private record Entry(String userId, Instant expiresAt) {
    }

    private final Map<String, Entry> tokens = new ConcurrentHashMap<>();

    public void store(String jti, String userId, long ttlMillis) {
        tokens.put(jti, new Entry(userId, Instant.now().plusMillis(ttlMillis)));
    }

    /**
     * Returns the userId for a still-valid token, WITHOUT consuming it.
     */
    public Optional<String> peek(String jti) {
        Entry entry = tokens.get(jti);
        if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
            return Optional.empty();
        }
        return Optional.of(entry.userId());
    }

    /**
     * Consumes (removes) a token, returning the userId if it was valid.
     * Used during refresh-token rotation so a token can only be used once.
     */
    public Optional<String> consume(String jti) {
        Entry entry = tokens.remove(jti);
        if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
            return Optional.empty();
        }
        return Optional.of(entry.userId());
    }

    public void revoke(String jti) {
        tokens.remove(jti);
    }

    @Scheduled(fixedRate = 60_000)
    public void evictExpired() {
        Instant now = Instant.now();
        tokens.entrySet().removeIf(e -> e.getValue().expiresAt().isBefore(now));
    }
}
