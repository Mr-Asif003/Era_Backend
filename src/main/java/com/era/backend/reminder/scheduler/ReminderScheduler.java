package com.era.backend.reminder.scheduler;

import com.era.backend.reminder.service.ReminderService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Polls for due reminders. @EnableScheduling is already on EraApplication. */
@Component
@RequiredArgsConstructor
public class ReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);

    private final ReminderService reminderService;

    @Scheduled(fixedDelay = 30_000, initialDelay = 10_000)
    public void fireDueReminders() {
        int fired = reminderService.processDueReminders();
        if (fired > 0) log.debug("Delivered {} reminder(s)", fired);
    }
}
