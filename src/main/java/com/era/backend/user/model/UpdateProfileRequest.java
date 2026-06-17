package com.era.backend.user.model;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String displayName;
    private String bio;
    private String avatarUrl;
    private String avatarColor;
    private String eraVoice;
    private String eraLanguage;
}
