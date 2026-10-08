package com.era.backend.reminder.enums;

/** Matches the filter chips on the Reminders screen. */
public enum ReminderFilter {
    ALL,
    TODAY,     // not done, due today or already overdue
    UPCOMING,  // not done, due after today
    DONE
}
