package com.era.backend.common;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Universal API response envelope used by every controller.
 * { success, message, data, timestamp }
 */
@Data
@Builder
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private String timestamp;

    public static <T> ApiResponse<T> ok(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(Instant.now().toString())
                .build();
    }

    public static <T> ApiResponse<T> fail(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .timestamp(Instant.now().toString())
                .build();
    }
}
