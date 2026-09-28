package com.era.backend.task.service;

import com.era.backend.common.ApiResponse;
import com.era.backend.task.enums.TaskStatus;
import com.era.backend.task.enums.Priority;
import com.era.backend.task.model.Task;
import com.era.backend.task.enums.TaskStatus;
import com.era.backend.task.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    // =========================================================
    // CREATE TASK
    // =========================================================

    public ApiResponse<Task> createTask(Task task) {

        // Validate task dates
        validateTaskDates(
                task.getStartDateTime(),
                task.getEndDateTime()
        );

        // Default status
        if (task.getStatus() == null) {
            task.setStatus(TaskStatus.TODO);
        }

        // Default created time
        if (task.getCreatedAt() == null) {
            task.setCreatedAt(LocalDateTime.now());
        }

        // Updated time
        task.setUpdatedAt(LocalDateTime.now());

        Task savedTask = taskRepository.save(task);

        return ApiResponse.<Task>builder()
                .success(true)
                .message("Task created successfully")
                .data(savedTask)
                .build();
    }


    // =========================================================
    // GET TASK BY ID
    // =========================================================

    public ApiResponse<Task> getTaskById(String taskId) {

        return taskRepository.findById(taskId)
                .map(task -> ApiResponse.<Task>builder()
                        .success(true)
                        .message("Task fetched successfully")
                        .data(task)
                        .build())
                .orElseGet(() -> ApiResponse.<Task>builder()
                        .success(false)
                        .message("Task not found")
                        .data(null)
                        .build());
    }


    // =========================================================
    // GET ALL USER TASKS
    // =========================================================

    public ApiResponse<List<Task>> getUserTasks(String userId) {

        List<Task> tasks = taskRepository.findByUserId(userId);

        return ApiResponse.<List<Task>>builder()
                .success(true)
                .message("Tasks fetched successfully")
                .data(tasks)
                .build();
    }


    // =========================================================
    // UPDATE TASK
    // =========================================================

    public ApiResponse<Task> updateTask(String taskId, Task updatedTask) {

        return taskRepository.findById(taskId)
                .map(existingTask -> {

                    // Update basic fields
                    existingTask.setTitle(updatedTask.getTitle());
                    existingTask.setDescription(updatedTask.getDescription());

                    // Update priority
                    if (updatedTask.getPriority() != null) {
                        existingTask.setPriority(
                                updatedTask.getPriority()
                        );
                    }

                    // Update status
                    if (updatedTask.getStatus() != null) {
                        existingTask.setStatus(
                                updatedTask.getStatus()
                        );
                    }

                    // Update dates
                    if (updatedTask.getStartDateTime() != null) {
                        existingTask.setStartDateTime(
                                updatedTask.getStartDateTime()
                        );
                    }

                    if (updatedTask.getEndDateTime() != null) {
                        existingTask.setEndDateTime(
                                updatedTask.getEndDateTime()
                        );
                    }

                    // Validate dates
                    validateTaskDates(
                            existingTask.getStartDateTime(),
                            existingTask.getEndDateTime()
                    );

                    // Update timestamp
                    existingTask.setUpdatedAt(
                            LocalDateTime.now()
                    );

                    Task savedTask = taskRepository.save(existingTask);

                    return ApiResponse.<Task>builder()
                            .success(true)
                            .message("Task updated successfully")
                            .data(savedTask)
                            .build();
                })
                .orElseGet(() -> ApiResponse.<Task>builder()
                        .success(false)
                        .message("Task not found")
                        .data(null)
                        .build());
    }


    // =========================================================
    // DELETE TASK
    // =========================================================

    public ApiResponse<?> deleteTask(String taskId) {

        if (!taskRepository.existsById(taskId)) {
            return ApiResponse.builder()
                    .success(false)
                    .message("Task not found")
                    .data(null)
                    .build();
        }

        taskRepository.deleteById(taskId);

        return ApiResponse.builder()
                .success(true)
                .message("Task deleted successfully")
                .data(null)
                .build();
    }


    // =========================================================
    // COMPLETE TASK
    // =========================================================

    public ApiResponse<Task> completeTask(String taskId) {

        return taskRepository.findById(taskId)
                .map(task -> {

                    task.setStatus(TaskStatus.DONE);

                    task.setCompletedAt(
                            LocalDateTime.now()
                    );

                    task.setUpdatedAt(
                            LocalDateTime.now()
                    );

                    Task savedTask = taskRepository.save(task);

                    return ApiResponse.<Task>builder()
                            .success(true)
                            .message("Task completed successfully")
                            .data(savedTask)
                            .build();
                })
                .orElseGet(() -> ApiResponse.<Task>builder()
                        .success(false)
                        .message("Task not found")
                        .data(null)
                        .build());
    }



    // =========================================================
    // START TASK
    // =========================================================

    public ApiResponse<Task> startTask(String taskId) {

        return taskRepository.findById(taskId)
                .map(task -> {

                    task.setStatus(TaskStatus.IN_PROGRESS);

                    task.setUpdatedAt(
                            LocalDateTime.now()
                    );

                    Task savedTask = taskRepository.save(task);

                    return ApiResponse.<Task>builder()
                            .success(true)
                            .message("Task started successfully")
                            .data(savedTask)
                            .build();
                })
                .orElseGet(() -> ApiResponse.<Task>builder()
                        .success(false)
                        .message("Task not found")
                        .data(null)
                        .build());
    }


    // =========================================================
    // GET TASKS BY STATUS
    // =========================================================

    public ApiResponse<List<Task>> getTasksByStatus(
            String userId,
            TaskStatus status
    ) {

        List<Task> tasks =
                taskRepository.findByUserIdAndStatus(
                        userId,
                        status
                );

        return ApiResponse.<List<Task>>builder()
                .success(true)
                .message("Tasks fetched successfully")
                .data(tasks)
                .build();
    }


    // =========================================================
    // GET TASKS BY PRIORITY
    // =========================================================

    public ApiResponse<List<Task>> getTasksByPriority(
            String userId,
            Priority priority
    ) {

        List<Task> tasks =
                taskRepository.findByUserIdAndPriority(
                        userId,
                        priority
                );

        return ApiResponse.<List<Task>>builder()
                .success(true)
                .message("Tasks fetched successfully")
                .data(tasks)
                .build();
    }


    // =========================================================
    // GET OVERDUE TASKS
    // =========================================================

    public ApiResponse<List<Task>> getOverdueTasks(
            String userId
    ) {

        LocalDateTime now = LocalDateTime.now();

        List<Task> tasks =
                taskRepository.findOverdueTasks(
                        userId,
                        now,
                        TaskStatus.DONE
                );

        return ApiResponse.<List<Task>>builder()
                .success(true)
                .message("Overdue tasks fetched successfully")
                .data(tasks)
                .build();
    }


    // =========================================================
    // GET UPCOMING TASKS
    // =========================================================

    public ApiResponse<List<Task>> getUpcomingTasks(
            String userId
    ) {

        LocalDateTime now = LocalDateTime.now();

        List<Task> tasks =
                taskRepository.findUpcomingTasks(
                        userId,
                        now
                );

        return ApiResponse.<List<Task>>builder()
                .success(true)
                .message("Upcoming tasks fetched successfully")
                .data(tasks)
                .build();
    }


    // =========================================================
    // SEARCH TASKS
    // =========================================================

    public ApiResponse<List<Task>> searchTasks(
            String userId,
            String title
    ) {

        List<Task> tasks =
                taskRepository
                        .findByUserIdAndTitleContainingIgnoreCase(
                                userId,
                                title
                        );

        return ApiResponse.<List<Task>>builder()
                .success(true)
                .message("Tasks searched successfully")
                .data(tasks)
                .build();
    }


    // =========================================================
    // VALIDATE TASK DATES
    // =========================================================

    private void validateTaskDates(
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    ) {

        if (startDateTime != null
                && endDateTime != null
                && endDateTime.isBefore(startDateTime)) {

            throw new IllegalArgumentException(
                    "End date/time cannot be before start date/time"
            );
        }
    }
}