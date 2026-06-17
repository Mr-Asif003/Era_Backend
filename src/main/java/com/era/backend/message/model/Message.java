package com.era.backend.message.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "messages")
@CompoundIndex(name = "conversation_createdAt_idx", def = "{'conversationId': 1, 'createdAt': -1}")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    @Id
    private String id;

    private String conversationId;
    private String senderId;
    private String content;

    private MessageType type;          // TEXT|IMAGE|VOICE|FILE|ERA
    private MessageStatus status;      // SENT|DELIVERED|READ

    private String replyToId;          // null if not a reply
    private String fileUrl;            // S3 URL for media
    private Long fileSizeBytes;
    private Integer voiceDurationSecs;

    private boolean deleted;

    private Instant createdAt;
    private Instant updatedAt;
}
