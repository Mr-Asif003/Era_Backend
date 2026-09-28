package com.era.backend.task.model;

import com.era.backend.task.enums.Priority;
import com.era.backend.task.enums.TaskCreator;
import com.era.backend.task.enums.TaskStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task {
    private String id;

    private String title;

    private String description;

    private Priority priority;

    private TaskStatus status;

    private LocalDateTime startDateTime;

    private LocalDateTime endDateTime;

    private TaskCreator createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime completedAt;

    private String userId;
}