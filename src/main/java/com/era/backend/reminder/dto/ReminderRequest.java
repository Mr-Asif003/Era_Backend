package com.era.backend.reminder.dto;

import com.era.backend.reminder.enums.RepeatType;
import com.era.backend.task.enums.Priority;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;

/** Create (POST) and update (PUT). On update, null fields are left unchanged. */
@Data
public class ReminderRequest {

    @Size(max = 200, message = "Title must be at most 200 characters")
    private String title;

    /** ISO-8601 instant, e.g. 2026-10-01T09:30:00Z. Required on create. */
    private Instant remindAt;

    /** IANA zone id, e.g. "Asia/Kolkata". Defaults to UTC. */
    private String timezone;

    private RepeatType repeat;
    private Priority priority;
}
