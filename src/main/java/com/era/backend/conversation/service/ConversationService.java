package com.era.backend.conversation.service;

import com.era.backend.conversation.dto.CreateConversationRequest;
import com.era.backend.conversation.model.Conversation;
import com.era.backend.conversation.model.MemberRole;
import com.era.backend.conversation.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;

    public List<Conversation> getConversationsForUser(String userId) {
        return conversationRepository.findAllByMemberId(userId).stream()
                .sorted(Comparator.comparing(
                        (Conversation c) -> c.getLastMessage() != null
                                ? c.getLastMessage().getTimestamp()
                                : c.getUpdatedAt(),
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .collect(Collectors.toList());
    }

    public Conversation create(String creatorId, CreateConversationRequest req) {
        Instant now = Instant.now();

        List<Conversation.Member> members = Stream.concat(
                Stream.of(creatorId),
                req.getMemberIds() == null ? Stream.empty() : req.getMemberIds().stream()
        ).distinct().map(userId -> Conversation.Member.builder()
                .userId(userId)
                .role(userId.equals(creatorId) ? MemberRole.ADMIN : MemberRole.MEMBER)
                .joinedAt(now)
                .muted(false)
                .build()
        ).collect(Collectors.toList());

        Conversation conversation = Conversation.builder()
                .type(req.getType())
                .name(req.getName())
                .createdBy(creatorId)
                .members(members)
                .createdAt(now)
                .updatedAt(now)
                .build();

        return conversationRepository.save(conversation);
    }

    public Conversation getById(String conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new NoSuchElementException("Conversation not found"));
    }
}
