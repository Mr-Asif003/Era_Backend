package com.era.backend.task.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TaskSummaryResponse {
    private long total;
    private long todo;
    private long inProgress;
    private long done;
    private long overdue;
}
