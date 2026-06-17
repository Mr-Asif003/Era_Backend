package com.era.backend.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Key: presence:{userId}   Value: "online"   TTL: 30s
 * A heartbeat from the client keeps the key alive; if the client
 * disconnects without sending one, the key naturally expires.
 */
@Service
@RequiredArgsConstructor
public class PresenceService {

    private final StringRedisTemplate redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    private static final Duration PRESENCE_TTL = Duration.ofSeconds(30);
    private static final Duration LAST_SEEN_TTL = Duration.ofDays(30);

    public void setOnline(String userId) {
        redisTemplate.opsForValue().set("presence:" + userId, "online", PRESENCE_TTL);
        broadcast(userId, "online");
    }

    public void heartbeat(String userId) {
        redisTemplate.opsForValue().set("presence:" + userId, "online", PRESENCE_TTL);
    }

    public void setOffline(String userId) {
        redisTemplate.delete("presence:" + userId);
        redisTemplate.opsForValue().set("lastseen:" + userId, Instant.now().toString(), LAST_SEEN_TTL);
        broadcast(userId, "offline");
    }

    public boolean isOnline(String userId) {
        return redisTemplate.opsForValue().get("presence:" + userId) != null;
    }

    private void broadcast(String userId, String status) {
        messagingTemplate.convertAndSend("/topic/presence",
                Map.of("userId", userId, "status", status, "lastSeen", Instant.now().toString()));
    }
}
