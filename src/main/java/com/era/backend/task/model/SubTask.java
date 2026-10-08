package com.era.backend.task.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Embedded inside {@link Task#getSubTasks()} - not its own collection, so it
 * has no taskId back-reference and its id is a plain UUID string.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubTask {
    private String id;
    private String title;
    private boolean completed;
    private Instant completedAt;
    private Instant createdAt;
}
