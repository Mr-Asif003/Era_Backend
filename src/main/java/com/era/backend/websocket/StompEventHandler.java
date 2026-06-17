package com.era.backend.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

/**
 * Marks a user online the moment their STOMP session connects, and offline
 * the moment it disconnects (browser closed, app backgrounded, network
 * drop, etc). Combined with the Redis TTL in PresenceService, this gives an
 * accurate, self-healing presence system.
 */
@Component
@RequiredArgsConstructor
public class StompEventHandler {

    private final PresenceService presenceService;

    @EventListener
    public void onSessionConnected(SessionConnectedEvent event) {
        Principal user = event.getUser();
        if (user != null) {
            presenceService.setOnline(user.getName());
        }
    }

    @EventListener
    public void onSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = accessor.getUser();
        if (user != null) {
            presenceService.setOffline(user.getName());
        }
    }
}
