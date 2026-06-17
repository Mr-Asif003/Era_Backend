package com.era.backend.message.dto;

import com.era.backend.message.model.MessageType;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendMessageRequest {

    @NotBlank
    private String conversationId;

    @NotBlank
    private String text;

    private MessageType type = MessageType.TEXT;
    private String replyToId;
}
