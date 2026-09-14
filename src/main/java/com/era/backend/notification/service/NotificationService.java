package com.era.backend.notification.service;

import com.era.backend.message.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Placeholder for push notification delivery to offline users. Wire up
 * Firebase Cloud Messaging (or similar) here once device tokens are being
 * collected - swap the log line for a real push call.
 *
 * Called directly (in-process) rather than through a message queue: at
 * this app's current scale a queue only adds operational overhead
 * (a broker to run, topics to manage) without a real benefit yet.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    public void notifyOfflineUser(String userId, Message message) {
        log.info("[PUSH] -> user {} : new message {} in conversation {}",
                userId, message.getId(), message.getConversationId());
    }
}
