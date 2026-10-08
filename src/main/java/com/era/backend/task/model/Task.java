package com.era.backend.task.model;

import com.era.backend.task.enums.Priority;
import com.era.backend.task.enums.TaskCreator;
import com.era.backend.task.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "tasks")
@CompoundIndex(name = "user_status_idx", def = "{'userId': 1, 'status': 1}")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String title;
    private String description;

    private Priority priority;
    private TaskStatus status;

    private Instant startDateTime;
    private Instant endDateTime;

    private TaskCreator createdBy;

    @Builder.Default
    private List<SubTask> subTasks = new ArrayList<>();

    private Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;
}
