package com.era.backend.task.repository;

import com.era.backend.task.model.Task;
import com.era.backend.task.enums.TaskStatus;
import com.era.backend.task.enums.Priority;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface TaskRepository
        extends MongoRepository<Task, String> {

    // Get all tasks of a user
    List<Task> findByUserId(String userId);

    // Get tasks by status
    List<Task> findByUserIdAndStatus(
            String userId,
            TaskStatus status
    );

    // Get tasks by priority
    List<Task> findByUserIdAndPriority(
            String userId,
            Priority priority
    );

    // Search by title
    List<Task> findByUserIdAndTitleContainingIgnoreCase(
            String userId,
            String title
    );

    // Overdue tasks
    @Query("""
        {
            'userId': ?0,
            'endDateTime': { '$lt': ?1 },
            'status': { '$ne': ?2 }
        }
    """)
    List<Task> findOverdueTasks(
            String userId,
            LocalDateTime now,
            TaskStatus completedStatus
    );

    // Upcoming tasks
    @Query("""
        {
            'userId': ?0,
            'startDateTime': { '$gte': ?1 }
        }
    """)
    List<Task> findUpcomingTasks(
            String userId,
            LocalDateTime now
    );
}