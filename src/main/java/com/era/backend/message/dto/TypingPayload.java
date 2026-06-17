package com.era.backend.message.dto;

import lombok.Data;

@Data
public class TypingPayload {
    private String conversationId;
    private boolean isTyping;
}
