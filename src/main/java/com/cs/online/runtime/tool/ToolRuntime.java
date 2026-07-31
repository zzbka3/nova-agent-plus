package com.cs.online.runtime.tool;

import com.cs.online.context.Context;
import com.cs.online.execution.DefaultExecution;
import com.cs.online.execution.Execution;
import com.cs.online.execution.ExecutionStatus;
import com.cs.online.resource.Resource;
import com.cs.online.resource.ToolDefinition;
import com.cs.online.runtime.Runtime;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Tool 永远是最小能力。执行链路固定为 Validate -> Invoke -> Trace -> Return。
 */
@Component
public class ToolRuntime implements Runtime {

    private final ToolRegistry toolRegistry;

    public ToolRuntime(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    @Override
    public Execution execute(Resource resource, Context context) {
        if (!(resource instanceof ToolDefinition toolDefinition)) {
            throw new IllegalArgumentException("ToolRuntime can only execute ToolDefinition resources");
        }
        JsonNode args = context.variables().getValue("__tool_args__") instanceof JsonNode node
                ? node
                : null;
        return execute(toolDefinition, args, context);
    }

    public Execution execute(ToolDefinition toolDefinition, JsonNode args, Context context) {
        DefaultExecution execution = new DefaultExecution(UUID.randomUUID().toString(), context);
        execution.setStatus(ExecutionStatus.RUNNING);

        Tool tool = toolRegistry.get(toolDefinition.id());
        if (tool == null) {
            execution.setStatus(ExecutionStatus.FAILED);
            execution.setErrorMessage("Tool not registered: " + toolDefinition.id());
            context.observation().record("[Tool:" + toolDefinition.id() + "] not found");
            return execution;
        }

        // Validate
        if (args == null) {
            execution.setStatus(ExecutionStatus.FAILED);
            execution.setErrorMessage("Tool args must not be null");
            context.observation().record("[Tool:" + toolDefinition.id() + "] validate failed: args is null");
            return execution;
        }

        // Invoke
        ToolResult result;
        try {
            result = tool.invoke(args);
        } catch (Exception e) {
            execution.setStatus(ExecutionStatus.FAILED);
            execution.setErrorMessage(e.getMessage());
            context.observation().record("[Tool:" + toolDefinition.id() + "] invoke error: " + e.getMessage());
            return execution;
        }

        // Trace
        context.observation().record("[Tool:" + toolDefinition.id() + "] args=" + args + " result=" + result);

        // Return
        if (result.success()) {
            execution.setStatus(ExecutionStatus.SUCCESS);
            execution.setResult(result.data());
        } else {
            execution.setStatus(ExecutionStatus.FAILED);
            execution.setErrorMessage(result.errorMessage());
        }
        return execution;
    }
}
