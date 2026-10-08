package com.era.backend.reminder.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** Counts for the four chips at the top of the Reminders screen. */
@Data
@AllArgsConstructor
public class ReminderSummaryResponse {
    private long all;
    private long today;
    private long upcoming;
    private long done;
}
