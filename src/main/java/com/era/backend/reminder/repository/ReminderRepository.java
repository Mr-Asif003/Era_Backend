package com.era.backend.reminder.repository;

import com.era.backend.reminder.model.Reminder;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReminderRepository extends MongoRepository<Reminder, String> {

    Optional<Reminder> findByIdAndUserId(String id, String userId);

    List<Reminder> findByUserIdOrderByRemindAtAsc(String userId);

    // Everything the scheduler needs to deliver right now
    @Query("{ 'done': false, 'notified': false, 'remindAt': { '$lte': ?0 } }")
    List<Reminder> findDue(Instant now);
}
