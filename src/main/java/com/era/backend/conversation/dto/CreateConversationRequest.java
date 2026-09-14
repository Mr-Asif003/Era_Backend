package com.era.backend.conversation.dto;

import com.era.backend.conversation.model.ConversationType;
import lombok.Data;

import java.util.List;

@Data
public class CreateConversationRequest {
    private ConversationType type;     // DIRECT | GROUP
    private String name;               // required for GROUP
    private List<String> memberIds;    // other participants (not including the creator)
}
