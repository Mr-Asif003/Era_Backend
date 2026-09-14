package com.era.backend.websocket;

import com.era.backend.user.repository.UserRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which users currently have a live STOMP session, plus each user's
 * last-seen timestamp once they go offline. Also keeps the User document's
 * online/lastSeen fields in sync so any endpoint returning a User (not just
 * conversation responses, which read presence live from here directly)
 * reflects real presence too.
 *
 * In-memory session tracking: fine for a single backend instance. If this
 * backend is ever scaled horizontally, back the session-count map with a
 * shared store (Redis, etc) instead - the {@link #isOnline}/
 * {@link #getLastSeen} call sites won't need to change.
 */
@Service
public class PresenceService {

    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    // userId -> count of open STOMP sessions (a user can have >1 device/tab)
    private final Map<String, Integer> onlineSessionCounts = new ConcurrentHashMap<>();
    private final Map<String, Instant> lastSeen = new ConcurrentHashMap<>();

    public PresenceService(SimpMessagingTemplate messagingTemplate, UserRepository userRepository) {
        this.messagingTemplate = messagingTemplate;
        this.userRepository = userRepository;
    }

    public void connect(String userId) {
        int updated = onlineSessionCounts.merge(userId, 1, Integer::sum);
        if (updated == 1) {
            userRepository.findById(userId).ifPresent(user -> {
                user.setOnline(true);
                userRepository.save(user);
            });
            broadcast(userId, "online");
        }
    }

    public void disconnect(String userId) {
        Integer updated = onlineSessionCounts.computeIfPresent(userId, (id, count) -> count > 1 ? count - 1 : null);
        if (updated == null) {
            Instant now = Instant.now();
            lastSeen.put(userId, now);
            userRepository.findById(userId).ifPresent(user -> {
                user.setOnline(false);
                user.setLastSeen(now);
                userRepository.save(user);
            });
            broadcast(userId, "offline");
        }
    }

    public boolean isOnline(String userId) {
        return onlineSessionCounts.getOrDefault(userId, 0) > 0;
    }

    public Instant getLastSeen(String userId) {
        return lastSeen.get(userId);
    }

    private void broadcast(String userId, String status) {
        messagingTemplate.convertAndSend("/topic/presence",
                Map.of("userId", userId, "status", status, "lastSeen", Instant.now().toString()));
    }
}
