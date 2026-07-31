package com.cs.online.api.dto;

import com.cs.online.execution.DefaultExecution;
import com.cs.online.execution.Execution;
import com.cs.online.execution.ExecutionStatus;

public record ExecutionResponse(String executionId, ExecutionStatus status, Object result, String errorMessage) {

    public static ExecutionResponse from(Execution execution) {
        if (execution instanceof DefaultExecution de) {
            return new ExecutionResponse(de.executionId(), de.status(), de.getResult(), de.getErrorMessage());
        }
        return new ExecutionResponse(execution.executionId(), execution.status(), null, null);
    }
}
