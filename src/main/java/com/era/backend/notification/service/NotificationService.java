package com.era.backend.notification.service;

import com.era.backend.notification.kafka.MessageSentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Placeholder for push notification delivery. Wire up Firebase Cloud
 * Messaging (FCM) here for Phase 15 of the roadmap - swap the log line for
 * an actual FCM admin SDK call once device tokens are being collected.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    public void sendPushNotification(String userId, MessageSentEvent event) {
        log.info("[PUSH] -> user {} : new message {} in conversation {}",
                userId, event.getMessageId(), event.getConversationId());
        // TODO: integrate FCM admin SDK
    }
}
