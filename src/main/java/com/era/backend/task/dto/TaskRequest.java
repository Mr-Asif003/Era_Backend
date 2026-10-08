package com.era.backend.task.dto;

import com.era.backend.task.enums.Priority;
import com.era.backend.task.enums.TaskStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * Used for both create (POST) and update (PUT/PATCH). On update, null
 * fields are left unchanged.
 */
@Data
public class TaskRequest {

    // Required on create; on update the service ignores a null/blank title.
    @Size(max = 200, message = "Title must be at most 200 characters")
    private String title;

    @Size(max = 2000, message = "Description must be at most 2000 characters")
    private String description;

    private Priority priority;
    private TaskStatus status;
    private Instant startDateTime;
    private Instant endDateTime;

    /** Optional initial sub-tasks (create only). */
    @Valid
    private List<SubTaskRequest> subTasks;

    @Data
    public static class SubTaskRequest {
        @NotBlank(message = "Sub-task title is required")
        @Size(max = 200)
        private String title;
    }
}
