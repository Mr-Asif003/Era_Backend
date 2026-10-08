package com.era.backend.task.repository;

import com.era.backend.task.enums.Priority;
import com.era.backend.task.enums.TaskStatus;
import com.era.backend.task.model.Task;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends MongoRepository<Task, String> {

    // Every lookup is scoped by userId so one user can never touch another's task.
    Optional<Task> findByIdAndUserId(String id, String userId);

    List<Task> findByUserIdOrderByCreatedAtDesc(String userId);

    List<Task> findByUserIdAndStatusOrderByCreatedAtDesc(String userId, TaskStatus status);

    List<Task> findByUserIdAndPriorityOrderByCreatedAtDesc(String userId, Priority priority);

    List<Task> findByUserIdAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(String userId, String title);

    long countByUserIdAndStatus(String userId, TaskStatus status);

    long countByUserId(String userId);

    // Not done and past its end time
    @Query("{ 'userId': ?0, 'endDateTime': { '$lt': ?1 }, 'status': { '$ne': ?2 } }")
    List<Task> findOverdue(String userId, Instant now, TaskStatus doneStatus);

    @Query(value = "{ 'userId': ?0, 'endDateTime': { '$lt': ?1 }, 'status': { '$ne': ?2 } }", count = true)
    long countOverdue(String userId, Instant now, TaskStatus doneStatus);

    // Not done and starting (or due) from now on, soonest first
    @Query(value = "{ 'userId': ?0, 'status': { '$ne': ?2 }, "
            + "'$or': [ { 'startDateTime': { '$gte': ?1 } }, "
            + "{ 'startDateTime': null, 'endDateTime': { '$gte': ?1 } } ] }",
            sort = "{ 'startDateTime': 1, 'endDateTime': 1 }")
    List<Task> findUpcoming(String userId, Instant now, TaskStatus doneStatus);
}
