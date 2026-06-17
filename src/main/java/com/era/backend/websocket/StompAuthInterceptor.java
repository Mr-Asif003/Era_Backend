package com.era.backend.websocket;

import com.era.backend.auth.jwt.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Collections;
import java.util.List;

/**
 * Authenticates the STOMP CONNECT frame via the Authorization header.
 * No valid JWT -> connection is rejected before it's established.
 */
@Component
@RequiredArgsConstructor
public class StompAuthInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            List<String> authHeaders = accessor.getNativeHeader("Authorization");
            String token = (authHeaders != null && !authHeaders.isEmpty())
                    ? authHeaders.get(0).replace("Bearer ", "")
                    : null;

            if (token == null || !jwtUtil.isTokenValid(token)) {
                throw new IllegalArgumentException("Missing or invalid JWT on STOMP CONNECT");
            }

            Claims claims = jwtUtil.extractClaims(token);
            String userId = claims.getSubject();

            Principal principal = () -> userId; // simple Principal whose getName() == userId
            accessor.setUser(principal);
        }

        return message;
    }
}
