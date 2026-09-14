package com.era.backend.conversation.dto;

import com.era.backend.conversation.model.MemberRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * What the mobile client actually needs per member: not just the userId
 * stored in Mongo, but their current display name / avatar / online status,
 * joined in at read time. This matches the frontend's
 * `ConversationMember` type in conversation.types.ts exactly.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMemberResponse {
    private String userId;
    private String fullName;
    private String avatarUrl;
    private String avatarColor;
    private boolean online;
    private Instant lastSeen;
    private MemberRole role;
}
