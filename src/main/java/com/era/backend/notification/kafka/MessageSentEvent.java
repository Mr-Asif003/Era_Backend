package com.era.backend.notification.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Published to the "era.message.sent" Kafka topic whenever a message is
 * persisted. Consumed by NotificationConsumer to trigger push notifications
 * for recipients who are currently offline.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageSentEvent {
    private String messageId;
    private String senderId;
    private String conversationId;
}
