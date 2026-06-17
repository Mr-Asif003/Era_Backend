package com.era.backend.era.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EraMessage {
    private String role;       // "user" | "assistant"
    private String content;
    private Instant timestamp;
}
