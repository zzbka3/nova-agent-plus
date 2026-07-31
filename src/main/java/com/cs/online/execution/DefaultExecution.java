package com.cs.online.execution;

import com.cs.online.context.Context;

public class DefaultExecution implements Execution {

    private final String executionId;
    private final Context context;
    private ExecutionStatus status;
    private Object result;
    private String errorMessage;

    public DefaultExecution(String executionId, Context context) {
        this.executionId = executionId;
        this.context = context;
        this.status = ExecutionStatus.CREATED;
    }

    @Override
    public String executionId() {
        return executionId;
    }

    @Override
    public ExecutionStatus status() {
        return status;
    }

    @Override
    public Context context() {
        return context;
    }

    public void setStatus(ExecutionStatus status) {
        this.status = status;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
