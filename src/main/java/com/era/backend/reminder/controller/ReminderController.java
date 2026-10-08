package com.era.backend.reminder.controller;

import com.era.backend.common.ApiResponse;
import com.era.backend.reminder.dto.ReminderRequest;
import com.era.backend.reminder.dto.ReminderResponse;
import com.era.backend.reminder.dto.ReminderSummaryResponse;
import com.era.backend.reminder.enums.ReminderFilter;
import com.era.backend.reminder.service.ReminderService;
import com.era.backend.task.enums.TaskCreator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reminders")
@RequiredArgsConstructor
public class ReminderController {

    private final ReminderService reminderService;

    @PostMapping
    public ResponseEntity<ApiResponse<ReminderResponse>> create(@Valid @RequestBody ReminderRequest request) {
        ReminderResponse created = reminderService.create(currentUserId(), request, TaskCreator.USER);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(created, "Reminder created"));
    }

    /** GET /reminders?filter=TODAY&tz=Asia/Kolkata - both params optional (default ALL, UTC). */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ReminderResponse>>> list(
            @RequestParam(required = false, defaultValue = "ALL") ReminderFilter filter,
            @RequestParam(required = false) String tz) {
        return ResponseEntity.ok(ApiResponse.ok(reminderService.list(currentUserId(), filter, tz), "Reminders"));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<ReminderSummaryResponse>> summary(@RequestParam(required = false) String tz) {
        return ResponseEntity.ok(ApiResponse.ok(reminderService.summary(currentUserId(), tz), "Reminder summary"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReminderResponse>> get(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(reminderService.getById(currentUserId(), id), "Reminder"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ReminderResponse>> update(@PathVariable String id,
                                                                @Valid @RequestBody ReminderRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(reminderService.update(currentUserId(), id, request), "Reminder updated"));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<ReminderResponse>> toggle(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(reminderService.toggle(currentUserId(), id), "Reminder updated"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        reminderService.delete(currentUserId(), id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Reminder deleted"));
    }

    private String currentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
