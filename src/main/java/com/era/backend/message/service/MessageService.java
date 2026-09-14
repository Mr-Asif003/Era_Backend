package com.era.backend.message.service;

import com.era.backend.conversation.model.Conversation;
import com.era.backend.conversation.service.ConversationService;
import com.era.backend.message.dto.SendMessageRequest;
import com.era.backend.message.model.Message;
import com.era.backend.message.model.MessageStatus;
import com.era.backend.message.repository.MessageRepository;
import com.era.backend.notification.service.NotificationService;
import com.era.backend.websocket.PresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationService conversationService;
    private final SimpMessagingTemplate messagingTemplate;
    private final PresenceService presenceService;
    private final NotificationService notificationService;

    public Message sendMessage(String senderId, SendMessageRequest req) {
        Conversation conversation = conversationService.getEntityById(req.getConversationId());
        assertMember(conversation, senderId);

        Instant now = Instant.now();

        // 1. Persist
        Message message = messageRepository.save(Message.builder()
                .conversationId(req.getConversationId())
                .senderId(senderId)
                .content(req.getText())
                .type(req.getType())
                .status(MessageStatus.SENT)
                .replyToId(req.getReplyToId())
                .deleted(false)
                .createdAt(now)
                .updatedAt(now)
                .build());

        // 2. Update the conversation preview
        conversation.setLastMessage(Conversation.LastMessage.builder()
                .text(req.getText())
                .senderId(senderId)
                .timestamp(now)
                .build());
        conversation.setUpdatedAt(now);
        conversationService.save(conversation);

        // 3. Fan out over WebSocket - immediate delivery for online members,
        // and a direct (in-process) push-notification hook for offline ones.
        conversation.getMembers().stream()
                .map(Conversation.Member::getUserId)
                .filter(userId -> !userId.equals(senderId))
                .forEach(userId -> {
                    messagingTemplate.convertAndSendToUser(userId, "/queue/messages", message);
                    if (!presenceService.isOnline(userId)) {
                        notificationService.notifyOfflineUser(userId, message);
                    }
                });

        return message;
    }

    public Page<Message> getMessages(String conversationId, String requesterId, int page, int size) {
        Conversation conversation = conversationService.getEntityById(conversationId);
        assertMember(conversation, requesterId);

        return messageRepository.findByConversationIdAndDeletedFalseOrderByCreatedAtDesc(
                conversationId, PageRequest.of(page, size, Sort.by("createdAt").descending()));
    }

    public Message markAsRead(String messageId, String readerId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new NoSuchElementException("Message not found"));

        message.setStatus(MessageStatus.READ);
        message.setUpdatedAt(Instant.now());
        message = messageRepository.save(message);

        // Notify the sender their message was read
        messagingTemplate.convertAndSendToUser(
                message.getSenderId(), "/queue/delivery",
                Map.of("messageId", message.getId(), "status", "READ"));

        return message;
    }

    public void softDelete(String messageId, String requesterId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new NoSuchElementException("Message not found"));

        if (!message.getSenderId().equals(requesterId)) {
            throw new AccessDeniedException("You can only delete your own messages");
        }

        message.setDeleted(true);
        message.setUpdatedAt(Instant.now());
        messageRepository.save(message);
    }

    private void assertMember(Conversation conversation, String userId) {
        boolean isMember = conversation.getMembers().stream()
                .anyMatch(m -> m.getUserId().equals(userId));
        if (!isMember) {
            throw new AccessDeniedException("You are not a member of this conversation");
        }
    }
}
