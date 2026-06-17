package com.era.backend.user.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    @Indexed(unique = true)
    private String email;

    @JsonIgnore
    private String passwordHash;

    private String displayName;
    private String bio;
    private String avatarUrl;
    private String avatarColor;   // hex - used for gradient in mobile

    private boolean isOnline;
    private Instant lastSeen;

    private Instant createdAt;
    private Instant updatedAt;

    // Era AI preferences
    private String eraVoice;      // "default" | "calm" | "energetic"
    private String eraLanguage;   // "en" | "hi" | etc.
}
