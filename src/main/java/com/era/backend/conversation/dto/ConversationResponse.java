package com.era.backend.conversation.dto;

import com.era.backend.conversation.model.ConversationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponse {
    private String id;
    private ConversationType type;
    private String name;
    private String avatarUrl;
    private List<ConversationMemberResponse> members;
    private LastMessageResponse lastMessage;
    private Instant createdAt;
    private Instant updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LastMessageResponse {
        private String text;
        private String senderId;
        private Instant timestamp;
    }
}
