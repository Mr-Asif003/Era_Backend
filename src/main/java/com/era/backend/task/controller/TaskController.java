package com.era.backend.task.controller;

import com.era.backend.common.ApiResponse;
import com.era.backend.task.dto.TaskRequest;
import com.era.backend.task.dto.TaskResponse;
import com.era.backend.task.dto.TaskSummaryResponse;
import com.era.backend.task.enums.Priority;
import com.era.backend.task.enums.TaskCreator;
import com.era.backend.task.enums.TaskStatus;
import com.era.backend.task.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * The user id always comes from the JWT (never from the URL or body), so
 * a client can only see and change its own tasks.
 */
@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<ApiResponse<TaskResponse>> create(@Valid @RequestBody TaskRequest request) {
        TaskResponse task = taskService.create(currentUserId(), request, TaskCreator.USER);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(task, "Task created"));
    }

    /** GET /tasks?status=TODO&priority=HIGH&q=report - all params optional. */
    @GetMapping
    public ResponseEntity<ApiResponse<List<TaskResponse>>> list(
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false, name = "q") String query) {
        return ResponseEntity.ok(ApiResponse.ok(
                taskService.list(currentUserId(), status, priority, query), "Tasks"));
    }

    @GetMapping("/overdue")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> overdue() {
        return ResponseEntity.ok(ApiResponse.ok(taskService.overdue(currentUserId()), "Overdue tasks"));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> upcoming() {
        return ResponseEntity.ok(ApiResponse.ok(taskService.upcoming(currentUserId()), "Upcoming tasks"));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<TaskSummaryResponse>> summary() {
        return ResponseEntity.ok(ApiResponse.ok(taskService.summary(currentUserId()), "Task summary"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskResponse>> get(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(taskService.getById(currentUserId(), id), "Task"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskResponse>> update(@PathVariable String id,
                                                            @Valid @RequestBody TaskRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(taskService.update(currentUserId(), id, request), "Task updated"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        taskService.delete(currentUserId(), id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Task deleted"));
    }

    @PatchMapping("/{id}/start")
    public ResponseEntity<ApiResponse<TaskResponse>> start(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(taskService.start(currentUserId(), id), "Task started"));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<TaskResponse>> complete(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(taskService.complete(currentUserId(), id), "Task completed"));
    }

    @PatchMapping("/{id}/reopen")
    public ResponseEntity<ApiResponse<TaskResponse>> reopen(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(taskService.reopen(currentUserId(), id), "Task reopened"));
    }

    // ---- sub-tasks -------------------------------------------------------

    @PostMapping("/{id}/subtasks")
    public ResponseEntity<ApiResponse<TaskResponse>> addSubTask(@PathVariable String id,
                                                                @Valid @RequestBody TaskRequest.SubTaskRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                taskService.addSubTask(currentUserId(), id, body.getTitle()), "Sub-task added"));
    }

    @PatchMapping("/{id}/subtasks/{subTaskId}/toggle")
    public ResponseEntity<ApiResponse<TaskResponse>> toggleSubTask(@PathVariable String id,
                                                                   @PathVariable String subTaskId) {
        return ResponseEntity.ok(ApiResponse.ok(
                taskService.toggleSubTask(currentUserId(), id, subTaskId), "Sub-task updated"));
    }

    @DeleteMapping("/{id}/subtasks/{subTaskId}")
    public ResponseEntity<ApiResponse<TaskResponse>> deleteSubTask(@PathVariable String id,
                                                                   @PathVariable String subTaskId) {
        return ResponseEntity.ok(ApiResponse.ok(
                taskService.deleteSubTask(currentUserId(), id, subTaskId), "Sub-task deleted"));
    }

    private String currentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
