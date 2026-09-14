package com.era.backend.conversation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateDirectConversationRequest {

    @NotBlank
    @Email
    private String email;
}
