package com.era.backend.task.dto;

import com.era.backend.task.enums.Priority;
import com.era.backend.task.enums.TaskCreator;
import com.era.backend.task.enums.TaskStatus;
import com.era.backend.task.model.SubTask;
import com.era.backend.task.model.Task;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class TaskResponse {

    private String id;
    private String title;
    private String description;
    private Priority priority;
    private TaskStatus status;
    private Instant startDateTime;
    private Instant endDateTime;
    private TaskCreator createdBy;
    private List<SubTask> subTasks;
    private int subTasksTotal;
    private int subTasksCompleted;
    private boolean overdue;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;

    public static TaskResponse from(Task t) {
        List<SubTask> subs = t.getSubTasks() == null ? List.of() : t.getSubTasks();
        int done = (int) subs.stream().filter(SubTask::isCompleted).count();
        boolean overdue = t.getStatus() != TaskStatus.DONE
                && t.getEndDateTime() != null
                && t.getEndDateTime().isBefore(Instant.now());

        return TaskResponse.builder()
                .id(t.getId())
                .title(t.getTitle())
                .description(t.getDescription())
                .priority(t.getPriority())
                .status(t.getStatus())
                .startDateTime(t.getStartDateTime())
                .endDateTime(t.getEndDateTime())
                .createdBy(t.getCreatedBy())
                .subTasks(subs)
                .subTasksTotal(subs.size())
                .subTasksCompleted(done)
                .overdue(overdue)
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .completedAt(t.getCompletedAt())
                .build();
    }
}
