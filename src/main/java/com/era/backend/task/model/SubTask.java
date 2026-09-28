package com.era.backend.task.model;

import java.time.LocalDateTime;

public class SubTask {
    private Long id;

    private Long taskId;

    private String title;

    private boolean completed;


    private LocalDateTime completedAt;

    private LocalDateTime createdAt;
}
