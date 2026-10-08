package com.era.backend.task.service;

import com.era.backend.task.dto.TaskRequest;
import com.era.backend.task.dto.TaskResponse;
import com.era.backend.task.dto.TaskSummaryResponse;
import com.era.backend.task.enums.Priority;
import com.era.backend.task.enums.TaskCreator;
import com.era.backend.task.enums.TaskStatus;
import com.era.backend.task.model.SubTask;
import com.era.backend.task.model.Task;
import com.era.backend.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * All methods take the authenticated userId and only ever read/write that
 * user's tasks. Errors are thrown (NoSuchElementException -> 404,
 * IllegalArgumentException -> 400) and turned into the standard ApiResponse
 * envelope by GlobalExceptionHandler; controllers wrap successes.
 */
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;

    // ------------------------------------------------------------------ CRUD

    public TaskResponse create(String userId, TaskRequest req, TaskCreator creator) {
        if (req.getTitle() == null || req.getTitle().isBlank()) {
            throw new IllegalArgumentException("Title is required");
        }
        validateDates(req.getStartDateTime(), req.getEndDateTime());

        Instant now = Instant.now();
        TaskStatus status = req.getStatus() != null ? req.getStatus() : TaskStatus.TODO;

        List<SubTask> subs = new ArrayList<>();
        if (req.getSubTasks() != null) {
            req.getSubTasks().forEach(s -> subs.add(newSubTask(s.getTitle())));
        }

        Task task = Task.builder()
                .userId(userId)
                .title(req.getTitle().trim())
                .description(req.getDescription())
                .priority(req.getPriority() != null ? req.getPriority() : Priority.MEDIUM)
                .status(status)
                .startDateTime(req.getStartDateTime())
                .endDateTime(req.getEndDateTime())
                .createdBy(creator != null ? creator : TaskCreator.USER)
                .subTasks(subs)
                .createdAt(now)
                .updatedAt(now)
                .completedAt(status == TaskStatus.DONE ? now : null)
                .build();

        return TaskResponse.from(taskRepository.save(task));
    }

    public TaskResponse getById(String userId, String taskId) {
        return TaskResponse.from(find(userId, taskId));
    }

    public List<TaskResponse> list(String userId, TaskStatus status, Priority priority, String query) {
        List<Task> tasks;
        if (query != null && !query.isBlank()) {
            tasks = taskRepository.findByUserIdAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(userId, query.trim());
        } else if (status != null) {
            tasks = taskRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status);
        } else if (priority != null) {
            tasks = taskRepository.findByUserIdAndPriorityOrderByCreatedAtDesc(userId, priority);
        } else {
            tasks = taskRepository.findByUserIdOrderByCreatedAtDesc(userId);
        }
        // Allow status + priority (or query + filters) to combine
        return tasks.stream()
                .filter(t -> status == null || t.getStatus() == status)
                .filter(t -> priority == null || t.getPriority() == priority)
                .map(TaskResponse::from)
                .toList();
    }

    public TaskResponse update(String userId, String taskId, TaskRequest req) {
        Task task = find(userId, taskId);

        if (req.getTitle() != null && !req.getTitle().isBlank()) task.setTitle(req.getTitle().trim());
        if (req.getDescription() != null) task.setDescription(req.getDescription());
        if (req.getPriority() != null) task.setPriority(req.getPriority());
        if (req.getStartDateTime() != null) task.setStartDateTime(req.getStartDateTime());
        if (req.getEndDateTime() != null) task.setEndDateTime(req.getEndDateTime());
        if (req.getStatus() != null) applyStatus(task, req.getStatus());

        validateDates(task.getStartDateTime(), task.getEndDateTime());
        task.setUpdatedAt(Instant.now());
        return TaskResponse.from(taskRepository.save(task));
    }

    public void delete(String userId, String taskId) {
        taskRepository.delete(find(userId, taskId));
    }

    // ------------------------------------------------------- STATUS SHORTCUTS

    public TaskResponse start(String userId, String taskId) {
        return setStatus(userId, taskId, TaskStatus.IN_PROGRESS);
    }

    public TaskResponse complete(String userId, String taskId) {
        return setStatus(userId, taskId, TaskStatus.DONE);
    }

    public TaskResponse reopen(String userId, String taskId) {
        return setStatus(userId, taskId, TaskStatus.TODO);
    }

    private TaskResponse setStatus(String userId, String taskId, TaskStatus status) {
        Task task = find(userId, taskId);
        applyStatus(task, status);
        task.setUpdatedAt(Instant.now());
        return TaskResponse.from(taskRepository.save(task));
    }

    /** Keeps completedAt consistent with status (set on DONE, cleared otherwise). */
    private void applyStatus(Task task, TaskStatus status) {
        if (status == TaskStatus.DONE && task.getStatus() != TaskStatus.DONE) {
            task.setCompletedAt(Instant.now());
        } else if (status != TaskStatus.DONE) {
            task.setCompletedAt(null);
        }
        task.setStatus(status);
    }

    // ------------------------------------------------------------ SMART LISTS

    public List<TaskResponse> overdue(String userId) {
        return taskRepository.findOverdue(userId, Instant.now(), TaskStatus.DONE)
                .stream().map(TaskResponse::from).toList();
    }

    public List<TaskResponse> upcoming(String userId) {
        return taskRepository.findUpcoming(userId, Instant.now(), TaskStatus.DONE)
                .stream().map(TaskResponse::from).toList();
    }

    public TaskSummaryResponse summary(String userId) {
        return new TaskSummaryResponse(
                taskRepository.countByUserId(userId),
                taskRepository.countByUserIdAndStatus(userId, TaskStatus.TODO),
                taskRepository.countByUserIdAndStatus(userId, TaskStatus.IN_PROGRESS),
                taskRepository.countByUserIdAndStatus(userId, TaskStatus.DONE),
                taskRepository.countOverdue(userId, Instant.now(), TaskStatus.DONE));
    }

    // -------------------------------------------------------------- SUB-TASKS

    public TaskResponse addSubTask(String userId, String taskId, String title) {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("Sub-task title is required");
        Task task = find(userId, taskId);
        if (task.getSubTasks() == null) task.setSubTasks(new ArrayList<>());
        task.getSubTasks().add(newSubTask(title));
        return saveAfterSubTaskChange(task);
    }

    public TaskResponse toggleSubTask(String userId, String taskId, String subTaskId) {
        Task task = find(userId, taskId);
        SubTask sub = findSub(task, subTaskId);
        sub.setCompleted(!sub.isCompleted());
        sub.setCompletedAt(sub.isCompleted() ? Instant.now() : null);
        return saveAfterSubTaskChange(task);
    }

    public TaskResponse deleteSubTask(String userId, String taskId, String subTaskId) {
        Task task = find(userId, taskId);
        SubTask sub = findSub(task, subTaskId);
        task.getSubTasks().remove(sub);
        return saveAfterSubTaskChange(task);
    }

    /**
     * Sub-task progress drives the parent: all done -> DONE, first one
     * ticked -> IN_PROGRESS, un-ticking on a DONE task re-opens it.
     */
    private TaskResponse saveAfterSubTaskChange(Task task) {
        List<SubTask> subs = task.getSubTasks();
        if (subs != null && !subs.isEmpty()) {
            boolean all = subs.stream().allMatch(SubTask::isCompleted);
            boolean any = subs.stream().anyMatch(SubTask::isCompleted);
            if (all) applyStatus(task, TaskStatus.DONE);
            else if (any) applyStatus(task, TaskStatus.IN_PROGRESS);
            else if (task.getStatus() == TaskStatus.DONE) applyStatus(task, TaskStatus.TODO);
        }
        task.setUpdatedAt(Instant.now());
        return TaskResponse.from(taskRepository.save(task));
    }

    // ---------------------------------------------------------------- HELPERS

    private Task find(String userId, String taskId) {
        return taskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new NoSuchElementException("Task not found"));
    }

    private SubTask findSub(Task task, String subTaskId) {
        if (task.getSubTasks() == null) throw new NoSuchElementException("Sub-task not found");
        return task.getSubTasks().stream()
                .filter(s -> s.getId().equals(subTaskId))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Sub-task not found"));
    }

    private SubTask newSubTask(String title) {
        return SubTask.builder()
                .id(UUID.randomUUID().toString())
                .title(title.trim())
                .completed(false)
                .createdAt(Instant.now())
                .build();
    }

    private void validateDates(Instant start, Instant end) {
        if (start != null && end != null && end.isBefore(start)) {
            throw new IllegalArgumentException("End date/time cannot be before start date/time");
        }
    }
}
