package com.era.backend.reminder.service;

import com.era.backend.notification.service.NotificationService;
import com.era.backend.reminder.dto.ReminderRequest;
import com.era.backend.reminder.dto.ReminderResponse;
import com.era.backend.reminder.dto.ReminderSummaryResponse;
import com.era.backend.reminder.enums.ReminderFilter;
import com.era.backend.reminder.enums.RepeatType;
import com.era.backend.reminder.model.Reminder;
import com.era.backend.reminder.repository.ReminderRepository;
import com.era.backend.task.enums.Priority;
import com.era.backend.task.enums.TaskCreator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class ReminderService {

    private static final Logger log = LoggerFactory.getLogger(ReminderService.class);

    private final ReminderRepository reminderRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;

    // ------------------------------------------------------------------ CRUD

    public ReminderResponse create(String userId, ReminderRequest req, TaskCreator creator) {
        if (req.getTitle() == null || req.getTitle().isBlank()) {
            throw new IllegalArgumentException("Title is required");
        }
        if (req.getRemindAt() == null) {
            throw new IllegalArgumentException("remindAt is required");
        }
        String tz = resolveZone(req.getTimezone()).getId();
        RepeatType repeat = req.getRepeat() != null ? req.getRepeat() : RepeatType.NONE;

        Instant now = Instant.now();
        Instant remindAt = req.getRemindAt();
        if (remindAt.isBefore(now.minus(1, ChronoUnit.MINUTES))) {
            if (repeat == RepeatType.NONE) {
                throw new IllegalArgumentException("Reminder time must be in the future");
            }
            // A repeating reminder that starts in the past just begins at its next occurrence
            remindAt = nextOccurrence(remindAt, repeat, ZoneId.of(tz), now);
        }

        Reminder reminder = Reminder.builder()
                .userId(userId)
                .title(req.getTitle().trim())
                .remindAt(remindAt)
                .timezone(tz)
                .repeat(repeat)
                .priority(req.getPriority() != null ? req.getPriority() : Priority.MEDIUM)
                .done(false)
                .notified(false)
                .createdBy(creator != null ? creator : TaskCreator.USER)
                .createdAt(now)
                .updatedAt(now)
                .build();

        return ReminderResponse.from(reminderRepository.save(reminder));
    }

    public ReminderResponse getById(String userId, String id) {
        return ReminderResponse.from(find(userId, id));
    }

    public List<ReminderResponse> list(String userId, ReminderFilter filter, String tz) {
        ZoneId zone = resolveZone(tz);
        Instant endOfToday = endOfToday(zone);
        return reminderRepository.findByUserIdOrderByRemindAtAsc(userId).stream()
                .filter(r -> matches(r, filter, endOfToday))
                .map(ReminderResponse::from)
                .toList();
    }

    public ReminderSummaryResponse summary(String userId, String tz) {
        ZoneId zone = resolveZone(tz);
        Instant endOfToday = endOfToday(zone);
        List<Reminder> all = reminderRepository.findByUserIdOrderByRemindAtAsc(userId);
        return new ReminderSummaryResponse(
                all.size(),
                all.stream().filter(r -> matches(r, ReminderFilter.TODAY, endOfToday)).count(),
                all.stream().filter(r -> matches(r, ReminderFilter.UPCOMING, endOfToday)).count(),
                all.stream().filter(Reminder::isDone).count());
    }

    public ReminderResponse update(String userId, String id, ReminderRequest req) {
        Reminder r = find(userId, id);

        if (req.getTitle() != null && !req.getTitle().isBlank()) r.setTitle(req.getTitle().trim());
        if (req.getPriority() != null) r.setPriority(req.getPriority());
        if (req.getTimezone() != null) r.setTimezone(resolveZone(req.getTimezone()).getId());
        if (req.getRepeat() != null) r.setRepeat(req.getRepeat());

        if (req.getRemindAt() != null) {
            if (req.getRemindAt().isBefore(Instant.now().minus(1, ChronoUnit.MINUTES))
                    && r.getRepeat() == RepeatType.NONE) {
                throw new IllegalArgumentException("Reminder time must be in the future");
            }
            r.setRemindAt(req.getRemindAt());
            r.setNotified(false);   // new time -> deliver again
            r.setDone(false);
            r.setDoneAt(null);
        }

        r.setUpdatedAt(Instant.now());
        return ReminderResponse.from(reminderRepository.save(r));
    }

    public void delete(String userId, String id) {
        reminderRepository.delete(find(userId, id));
    }

    // ---------------------------------------------------------------- TOGGLE

    /**
     * The round check button in the UI.
     * - One-off reminder: flips done / not done.
     * - Repeating reminder: ticking it means "handled this occurrence", so it
     *   rolls forward to the next occurrence instead of being marked done.
     */
    public ReminderResponse toggle(String userId, String id) {
        Reminder r = find(userId, id);
        Instant now = Instant.now();

        if (!r.isDone() && r.getRepeat() != RepeatType.NONE) {
            r.setRemindAt(nextOccurrence(r.getRemindAt(), r.getRepeat(), resolveZone(r.getTimezone()), now));
            r.setNotified(false);
        } else if (r.isDone()) {
            r.setDone(false);
            r.setDoneAt(null);
            // Un-ticking an already-past one-off shouldn't instantly re-fire
            r.setNotified(!r.getRemindAt().isAfter(now));
        } else {
            r.setDone(true);
            r.setDoneAt(now);
        }

        r.setUpdatedAt(now);
        return ReminderResponse.from(reminderRepository.save(r));
    }

    // ------------------------------------------------------------- SCHEDULER

    /**
     * Called by ReminderScheduler. Delivers every due reminder over the
     * user's STOMP queue (/user/queue/reminders) and to NotificationService
     * (push hook), then either marks it delivered (one-off) or rolls it to
     * its next occurrence (daily/weekly).
     *
     * Note: single-instance safe. If you scale the backend horizontally,
     * claim each reminder atomically (findAndModify on notified=false) or
     * use ShedLock so two nodes don't deliver the same reminder.
     */
    public int processDueReminders() {
        Instant now = Instant.now();
        List<Reminder> due = reminderRepository.findDue(now);

        for (Reminder r : due) {
            try {
                ReminderResponse payload = ReminderResponse.from(r);
                messagingTemplate.convertAndSendToUser(r.getUserId(), "/queue/reminders", payload);
//                notificationService.notifyReminder(r.getUserId(), r);

                r.setLastFiredAt(now);
                if (r.getRepeat() == RepeatType.NONE) {
                    r.setNotified(true);
                } else {
                    r.setRemindAt(nextOccurrence(r.getRemindAt(), r.getRepeat(), resolveZone(r.getTimezone()), now));
                    r.setNotified(false);
                }
                r.setUpdatedAt(now);
                reminderRepository.save(r);
            } catch (Exception e) {
                // One bad reminder must never block the rest
                log.error("Failed to deliver reminder {}", r.getId(), e);
            }
        }
        return due.size();
    }

    // --------------------------------------------------------------- HELPERS

    private Reminder find(String userId, String id) {
        return reminderRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NoSuchElementException("Reminder not found"));
    }

    private boolean matches(Reminder r, ReminderFilter filter, Instant endOfToday) {
        if (filter == null) filter = ReminderFilter.ALL;
        return switch (filter) {
            case ALL -> true;
            case DONE -> r.isDone();
            case TODAY -> !r.isDone() && r.getRemindAt().isBefore(endOfToday);
            case UPCOMING -> !r.isDone() && !r.getRemindAt().isBefore(endOfToday);
        };
    }

    private Instant endOfToday(ZoneId zone) {
        return ZonedDateTime.now(zone).toLocalDate().plusDays(1).atStartOfDay(zone).toInstant();
    }

    /** First occurrence strictly after {@code after}, stepping by the repeat interval in the user's zone (DST-safe). */
    static Instant nextOccurrence(Instant from, RepeatType repeat, ZoneId zone, Instant after) {
        if (repeat == RepeatType.NONE) return from;
        long stepDays = repeat == RepeatType.DAILY ? 1 : 7;

        ZonedDateTime next = from.atZone(zone);
        ZonedDateTime target = after.atZone(zone);

        // Jump close to the target in one step, then walk the last few
        long gap = ChronoUnit.DAYS.between(next, target);
        if (gap > stepDays) next = next.plusDays((gap / stepDays) * stepDays);
        while (!next.toInstant().isAfter(after)) {
            next = next.plusDays(stepDays);
        }
        return next.toInstant();
    }

    private ZoneId resolveZone(String tz) {
        if (tz == null || tz.isBlank()) return ZoneId.of("UTC");
        try {
            return ZoneId.of(tz);
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("Invalid timezone: " + tz);
        }
    }
}
