package com.era.backend.notification.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishMessageSent(MessageSentEvent event) {
        // partition key = conversationId keeps events for the same conversation ordered
        kafkaTemplate.send("era.message.sent", event.getConversationId(), event);
    }
}
