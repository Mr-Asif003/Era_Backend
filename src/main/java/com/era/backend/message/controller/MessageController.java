package com.era.backend.message.controller;

import com.era.backend.common.ApiResponse;
import com.era.backend.common.PagedResponse;
import com.era.backend.conversation.model.Conversation;
import com.era.backend.conversation.service.ConversationService;
import com.era.backend.message.dto.SendMessageRequest;
import com.era.backend.message.dto.TypingPayload;
import com.era.backend.message.model.Message;
import com.era.backend.message.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final ConversationService conversationService;
    private final SimpMessagingTemplate messagingTemplate;

    // ── REST ─────────────────────────────────────────────────────────

    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<ApiResponse<PagedResponse<Message>>> getMessages(
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {

        Page<Message> result = messageService.getMessages(id, page, size);
        PagedResponse<Message> paged = PagedResponse.of(
                result.getContent(), page, size, result.getTotalElements());

        return ResponseEntity.ok(ApiResponse.ok(paged, "Messages"));
    }

    @PostMapping("/messages")
    public ResponseEntity<ApiResponse<Message>> sendMessage(@Valid @RequestBody SendMessageRequest request) {
        Message message = messageService.sendMessage(currentUserId(), request);
        return ResponseEntity.ok(ApiResponse.ok(message, "Message sent"));
    }

    @PutMapping("/messages/{id}/read")
    public ResponseEntity<ApiResponse<Message>> markAsRead(@PathVariable String id) {
        Message message = messageService.markAsRead(id, currentUserId());
        return ResponseEntity.ok(ApiResponse.ok(message, "Marked as read"));
    }

    @DeleteMapping("/messages/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        messageService.softDelete(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Message deleted"));
    }

    private String currentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    // ── STOMP (WebSocket) ────────────────────────────────────────────

    @MessageMapping("chat.send")
    public void handleChatSend(@Payload SendMessageRequest request, Principal principal) {
        messageService.sendMessage(principal.getName(), request);
    }

    @MessageMapping("chat.typing")
    public void handleTyping(@Payload TypingPayload payload, Principal principal) {
        Conversation conversation = conversationService.getById(payload.getConversationId());

        conversation.getMembers().stream()
                .map(Conversation.Member::getUserId)
                .filter(userId -> !userId.equals(principal.getName()))
                .forEach(userId -> messagingTemplate.convertAndSendToUser(
                        userId, "/queue/typing",
                        Map.of("conversationId", payload.getConversationId(),
                                "userId", principal.getName(),
                                "isTyping", payload.isTyping())));
    }
}
