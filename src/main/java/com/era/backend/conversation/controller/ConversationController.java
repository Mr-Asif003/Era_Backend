package com.era.backend.conversation.controller;

import com.era.backend.common.ApiResponse;
import com.era.backend.conversation.dto.ConversationResponse;
import com.era.backend.conversation.dto.CreateConversationRequest;
import com.era.backend.conversation.dto.CreateDirectConversationRequest;
import com.era.backend.conversation.model.ConversationType;
import com.era.backend.conversation.service.ConversationService;
import jakarta.validation.Valid;
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
    public ResponseEntity<ApiResponse<List<ConversationResponse>>> getAll() {
        List<ConversationResponse> conversations = conversationService.getConversationsForUser(currentUserId());
        return ResponseEntity.ok(ApiResponse.ok(conversations, "Conversations for current user"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ConversationResponse>> getById(@PathVariable String id) {
        ConversationResponse conversation = conversationService.getByIdForUser(id, currentUserId());
        return ResponseEntity.ok(ApiResponse.ok(conversation, "Conversation"));
    }

    @PostMapping("/direct")
    public ResponseEntity<ApiResponse<ConversationResponse>> createDirect(
            @Valid @RequestBody CreateDirectConversationRequest request) {
        ConversationResponse conversation = conversationService.createDirect(currentUserId(), request.getEmail());
        return ResponseEntity.ok(ApiResponse.ok(conversation, "Conversation ready"));
    }

    @PostMapping("/group")
    public ResponseEntity<ApiResponse<ConversationResponse>> createGroup(
            @RequestBody CreateConversationRequest request) {
        request.setType(ConversationType.GROUP);
        ConversationResponse conversation = conversationService.create(currentUserId(), request);
        return ResponseEntity.ok(ApiResponse.ok(conversation, "Group created"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ConversationResponse>> create(@RequestBody CreateConversationRequest request) {
        ConversationResponse conversation = conversationService.create(currentUserId(), request);
        return ResponseEntity.ok(ApiResponse.ok(conversation, "Conversation created"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> remove(@PathVariable String id) {
        conversationService.remove(id, currentUserId());
        return ResponseEntity.ok(ApiResponse.ok(null, "Conversation deleted"));
    }

    private String currentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
