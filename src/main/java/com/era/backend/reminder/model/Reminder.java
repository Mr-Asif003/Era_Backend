package com.era.backend.reminder.model;

import com.era.backend.reminder.enums.RepeatType;
import com.era.backend.task.enums.Priority;
import com.era.backend.task.enums.TaskCreator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "reminders")
// Used by the scheduler to find due reminders quickly
@CompoundIndex(name = "due_idx", def = "{'done': 1, 'notified': 1, 'remindAt': 1}")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Reminder {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String title;

    /** Next moment this reminder fires. Advances automatically for repeating reminders. */
    private Instant remindAt;

    /** IANA zone (e.g. "Asia/Kolkata"). Used for daily/weekly repeats (DST-safe) and today/upcoming buckets. */
    private String timezone;

    @Builder.Default
    private RepeatType repeat = RepeatType.NONE;

    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    private boolean done;
    private Instant doneAt;

    /** True once the current remindAt has been delivered (prevents re-firing). */
    private boolean notified;

    private Instant lastFiredAt;

    private TaskCreator createdBy;

    private Instant createdAt;
    private Instant updatedAt;
}
