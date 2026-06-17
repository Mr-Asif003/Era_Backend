package com.era.backend.era.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "era_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EraSession {

    @Id
    private String id;

    @Indexed
    private String userId;

    private List<EraMessage> history;

    private Instant createdAt;
    private Instant updatedAt;
}
