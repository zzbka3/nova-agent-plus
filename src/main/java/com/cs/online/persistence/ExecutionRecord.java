package com.cs.online.persistence;

import java.time.LocalDateTime;

public record ExecutionRecord(
        String executionId,
        String resourceId,
        String status,
        String contextJson,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
