package com.era.backend.notification.kafka;

import com.era.backend.conversation.model.Conversation;
import com.era.backend.conversation.repository.ConversationRepository;
import com.era.backend.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final ConversationRepository conversationRepository;
    private final NotificationService notificationService;
    private final StringRedisTemplate redisTemplate;

    @KafkaListener(topics = "era.message.sent", groupId = "notification-group")
    public void onMessageSent(MessageSentEvent event) {
        Conversation conversation = conversationRepository.findById(event.getConversationId())
                .orElseThrow();

        conversation.getMembers().stream()
                .filter(m -> !m.getUserId().equals(event.getSenderId()))
                .filter(m -> isOffline(m.getUserId()))
                .forEach(m -> notificationService.sendPushNotification(m.getUserId(), event));
    }

    private boolean isOffline(String userId) {
        return redisTemplate.opsForValue().get("presence:" + userId) == null;
    }
}
