package com.era.backend.message.service;

import com.era.backend.conversation.model.Conversation;
import com.era.backend.conversation.repository.ConversationRepository;
import com.era.backend.message.dto.SendMessageRequest;
import com.era.backend.message.model.Message;
import com.era.backend.message.model.MessageStatus;
import com.era.backend.message.repository.MessageRepository;
import com.era.backend.notification.kafka.MessageSentEvent;
import com.era.backend.notification.kafka.NotificationProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationProducer notificationProducer;

    public Message sendMessage(String senderId, SendMessageRequest req) {
        Conversation conversation = conversationRepository.findById(req.getConversationId())
                .orElseThrow(() -> new NoSuchElementException("Conversation not found"));

        Instant now = Instant.now();

        // 1. Save to MongoDB
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

        // Update the conversation preview
        conversation.setLastMessage(Conversation.LastMessage.builder()
                .text(req.getText())
                .senderId(senderId)
                .timestamp(now)
                .build());
        conversation.setUpdatedAt(now);
        conversationRepository.save(conversation);

        // 2. Push to other members via WebSocket (immediate for online users)
        conversation.getMembers().stream()
                .map(Conversation.Member::getUserId)
                .filter(userId -> !userId.equals(senderId))
                .forEach(userId -> messagingTemplate.convertAndSendToUser(userId, "/queue/messages", message));

        // 3. Emit Kafka event for push notifications (offline users)
        notificationProducer.publishMessageSent(
                new MessageSentEvent(message.getId(), senderId, message.getConversationId()));

        return message;
    }

    public Page<Message> getMessages(String conversationId, int page, int size) {
        return messageRepository.findByConversationIdAndDeletedFalseOrderByCreatedAtDesc(
                conversationId, PageRequest.of(page, size, Sort.by("createdAt").descending()));
    }

    public Message markAsRead(String messageId, String readerId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new NoSuchElementException("Message not found"));

        message.setStatus(MessageStatus.READ);
        message.setUpdatedAt(Instant.now());
        message = messageRepository.save(message);

        // Publish delivery receipt back to the sender
        messagingTemplate.convertAndSendToUser(
                message.getSenderId(), "/queue/delivery",
                Map.of("messageId", message.getId(), "status", "READ"));

        return message;
    }

    public void softDelete(String messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new NoSuchElementException("Message not found"));
        message.setDeleted(true);
        message.setUpdatedAt(Instant.now());
        messageRepository.save(message);
    }
}
