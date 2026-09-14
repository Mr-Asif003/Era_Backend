package com.era.backend.user.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    private String number;        // phone number
    private String bio;
    private String avatarUrl;
    private String avatarColor;   // hex - used for gradient in mobile

    // NOTE: fields are named without the "is" prefix on purpose. Lombok
    // generates isOnline()/isVerified() getters for them either way (that's
    // the standard boolean-getter convention), but if the *field* were also
    // named "isOnline" it creates an ambiguous property for Jackson (the
    // field's implicit name and the getter's implicit name diverge), which
    // can throw a "Conflicting getter definitions" error at serialization
    // time. @JsonProperty below restores the "isOnline"/"isVerified" wire
    // format the mobile app expects, without the naming clash.
    @JsonProperty("isOnline")
    private boolean online;

    private Instant lastSeen;

    @JsonProperty("isVerified")
    private boolean verified;

    private Instant createdAt;
    private Instant updatedAt;

    // Era AI preferences (kept for future use; Era chatbot itself is out of scope for now)
    private String eraVoice;      // "default" | "calm" | "energetic"
    private String eraLanguage;   // "en" | "hi" | etc.
}
