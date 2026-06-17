package com.era.backend.conversation.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "conversations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {

    @Id
    private String id;

    private ConversationType type;     // DIRECT | GROUP
    private String name;               // null for DIRECT
    private String avatarUrl;
    private String createdBy;

    private List<Member> members;
    private LastMessage lastMessage;

    private Instant createdAt;

    @Indexed
    private Instant updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Member {
        private String userId;
        private MemberRole role;       // MEMBER | ADMIN
        private Instant joinedAt;
        private boolean muted;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LastMessage {
        private String text;
        private String senderId;
        private Instant timestamp;
    }
}
