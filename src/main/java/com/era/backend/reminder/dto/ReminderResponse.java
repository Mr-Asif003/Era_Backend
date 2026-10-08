package com.era.backend.reminder.dto;

import com.era.backend.reminder.enums.RepeatType;
import com.era.backend.reminder.model.Reminder;
import com.era.backend.task.enums.Priority;
import com.era.backend.task.enums.TaskCreator;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ReminderResponse {

    private String id;
    private String title;
    private Instant remindAt;
    private String timezone;
    private RepeatType repeat;
    private Priority priority;
    private boolean done;
    private Instant doneAt;
    private TaskCreator createdBy;
    private Instant createdAt;
    private Instant updatedAt;

    public static ReminderResponse from(Reminder r) {
        return ReminderResponse.builder()
                .id(r.getId())
                .title(r.getTitle())
                .remindAt(r.getRemindAt())
                .timezone(r.getTimezone())
                .repeat(r.getRepeat())
                .priority(r.getPriority())
                .done(r.isDone())
                .doneAt(r.getDoneAt())
                .createdBy(r.getCreatedBy())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
