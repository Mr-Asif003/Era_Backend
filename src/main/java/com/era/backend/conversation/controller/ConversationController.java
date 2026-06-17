package com.era.backend.conversation.controller;

import com.era.backend.common.ApiResponse;
import com.era.backend.conversation.dto.CreateConversationRequest;
import com.era.backend.conversation.model.Conversation;
import com.era.backend.conversation.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Conversation>>> getAll() {
        List<Conversation> conversations = conversationService.getConversationsForUser(currentUserId());
        return ResponseEntity.ok(ApiResponse.ok(conversations, "Conversations for current user"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Conversation>> create(@RequestBody CreateConversationRequest request) {
        Conversation conversation = conversationService.create(currentUserId(), request);
        return ResponseEntity.ok(ApiResponse.ok(conversation, "Conversation created"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Conversation>> getById(@PathVariable String id) {
        Conversation conversation = conversationService.getById(id);
        return ResponseEntity.ok(ApiResponse.ok(conversation, "Conversation"));
    }

    private String currentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
