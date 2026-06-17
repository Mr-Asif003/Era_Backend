package com.era.backend.config;

import com.era.backend.websocket.StompAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthInterceptor stompAuthInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // In-memory broker for topics and user queues.
        // For production scale, swap this for a RabbitMQ/Redis relay (see architecture doc 11.2).
        registry.enableSimpleBroker("/topic", "/user");
        // Messages published to /app/* are routed to @MessageMapping methods
        registry.setApplicationDestinationPrefixes("/app");
        // For user-specific delivery (/user/queue/...)
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS(); // SockJS fallback - required by frontend
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Authenticate the STOMP CONNECT frame via JWT header
        registration.interceptors(stompAuthInterceptor);
    }
}
