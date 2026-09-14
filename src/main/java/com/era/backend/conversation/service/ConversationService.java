package com.era.backend.conversation.service;

import com.era.backend.conversation.dto.ConversationMemberResponse;
import com.era.backend.conversation.dto.ConversationResponse;
import com.era.backend.conversation.dto.CreateConversationRequest;
import com.era.backend.conversation.model.Conversation;
import com.era.backend.conversation.model.ConversationType;
import com.era.backend.conversation.model.MemberRole;
import com.era.backend.conversation.repository.ConversationRepository;
import com.era.backend.message.repository.MessageRepository;
import com.era.backend.user.model.User;
import com.era.backend.user.repository.UserRepository;
import com.era.backend.websocket.PresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final PresenceService presenceService;

    // ── Reads ────────────────────────────────────────────────────────

    public List<ConversationResponse> getConversationsForUser(String userId) {
        List<Conversation> conversations = conversationRepository.findAllByMemberId(userId).stream()
                .sorted(Comparator.comparing(
                        (Conversation c) -> c.getLastMessage() != null
                                ? c.getLastMessage().getTimestamp()
                                : c.getUpdatedAt(),
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .collect(Collectors.toList());

        return enrich(conversations);
    }

    public ConversationResponse getByIdForUser(String conversationId, String requesterId) {
        Conversation conversation = getEntityById(conversationId);
        assertMember(conversation, requesterId);
        return enrich(List.of(conversation)).get(0);
    }

    /**
     * Raw entity accessor used internally (e.g. by MessageService to fan
     * out over WebSocket) - no enrichment/DTO overhead needed there.
     */
    public Conversation getEntityById(String conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new NoSuchElementException("Conversation not found"));
    }

    /**
     * Persists changes to a raw entity (e.g. MessageService updating the
     * last-message preview after a new message is sent).
     */
    public Conversation save(Conversation conversation) {
        return conversationRepository.save(conversation);
    }

    // ── Writes ───────────────────────────────────────────────────────

    public ConversationResponse createDirect(String creatorId, String otherUserEmail) {
        User other = userRepository.findByEmail(otherUserEmail.trim().toLowerCase())
                .orElseThrow(() -> new NoSuchElementException("No user found with that email"));

        if (other.getId().equals(creatorId)) {
            throw new IllegalArgumentException("You can't start a conversation with yourself");
        }

        Conversation existing = conversationRepository
                .findDirectBetween(creatorId, other.getId(), ConversationType.DIRECT)
                .orElse(null);

        if (existing != null) {
            return enrich(List.of(existing)).get(0);
        }

        Instant now = Instant.now();
        List<Conversation.Member> members = List.of(
                Conversation.Member.builder().userId(creatorId).role(MemberRole.ADMIN).joinedAt(now).muted(false).build(),
                Conversation.Member.builder().userId(other.getId()).role(MemberRole.MEMBER).joinedAt(now).muted(false).build()
        );

        Conversation conversation = conversationRepository.save(Conversation.builder()
                .type(ConversationType.DIRECT)
                .createdBy(creatorId)
                .members(members)
                .createdAt(now)
                .updatedAt(now)
                .build());

        return enrich(List.of(conversation)).get(0);
    }

    public ConversationResponse create(String creatorId, CreateConversationRequest req) {
        if (req.getType() == ConversationType.GROUP
                && (req.getName() == null || req.getName().isBlank())) {
            throw new IllegalArgumentException("Group conversations require a name");
        }

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

        return enrich(List.of(conversationRepository.save(conversation))).get(0);
    }

    public void remove(String conversationId, String requesterId) {
        Conversation conversation = getEntityById(conversationId);
        assertMember(conversation, requesterId);

        messageRepository.deleteByConversationId(conversationId);
        conversationRepository.delete(conversation);
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private void assertMember(Conversation conversation, String userId) {
        boolean isMember = conversation.getMembers().stream()
                .anyMatch(m -> m.getUserId().equals(userId));
        if (!isMember) {
            throw new AccessDeniedException("You are not a member of this conversation");
        }
    }

    private List<ConversationResponse> enrich(List<Conversation> conversations) {
        Set<String> userIds = conversations.stream()
                .flatMap(c -> c.getMembers().stream())
                .map(Conversation.Member::getUserId)
                .collect(Collectors.toSet());

        Map<String, User> usersById = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a, HashMap::new));

        return conversations.stream().map(c -> ConversationResponse.builder()
                .id(c.getId())
                .type(c.getType())
                .name(c.getName())
                .avatarUrl(c.getAvatarUrl())
                .members(c.getMembers().stream().map(m -> toMemberResponse(m, usersById.get(m.getUserId())))
                        .collect(Collectors.toList()))
                .lastMessage(c.getLastMessage() == null ? null : ConversationResponse.LastMessageResponse.builder()
                        .text(c.getLastMessage().getText())
                        .senderId(c.getLastMessage().getSenderId())
                        .timestamp(c.getLastMessage().getTimestamp())
                        .build())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build()
        ).collect(Collectors.toList());
    }

    private ConversationMemberResponse toMemberResponse(Conversation.Member member, User user) {
        boolean online = presenceService.isOnline(member.getUserId());
        return ConversationMemberResponse.builder()
                .userId(member.getUserId())
                .fullName(user != null ? user.getDisplayName() : "Unknown User")
                .avatarUrl(user != null ? user.getAvatarUrl() : null)
                .avatarColor(user != null ? user.getAvatarColor() : null)
                .online(online)
                .lastSeen(online ? null : (user != null ? presenceService.getLastSeen(member.getUserId()) : null))
                .role(member.getRole())
                .build();
    }
}
