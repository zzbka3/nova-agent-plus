package com.cs.online.execution;

import com.cs.online.context.Context;

public interface Execution {

    String executionId();

    ExecutionStatus status();

    Context context();
}
