package com.cs.online.runtime.agent;

import com.cs.online.context.Context;
import com.cs.online.execution.DefaultExecution;
import com.cs.online.execution.Execution;
import com.cs.online.execution.ExecutionStatus;
import com.cs.online.resource.AgentDefinition;
import com.cs.online.resource.Resource;
import com.cs.online.resource.ToolDefinition;
import com.cs.online.runtime.ExecutorDispatcher;
import com.cs.online.runtime.Runtime;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * AgentRuntime 是整个系统唯一的调度入口，负责 ReAct 循环：
 * User -> Planner -> Action -> Dispatcher -> Observation -> Planner -> Finish。
 * 允许递归：一个 Agent 内部的 Action 也可以是另一个 Agent（Multi-Agent 的基础）。
 */
@Component
public class AgentRuntime implements Runtime {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Planner planner;
    private final ExecutorDispatcher dispatcher;

    public AgentRuntime(Planner planner, @Lazy ExecutorDispatcher dispatcher) {
        this.planner = planner;
        this.dispatcher = dispatcher;
    }

    @Override
    public Execution execute(Resource resource, Context context) {
        if (!(resource instanceof AgentDefinition agent)) {
            throw new IllegalArgumentException("AgentRuntime can only execute AgentDefinition resources");
        }

        DefaultExecution execution = new DefaultExecution(UUID.randomUUID().toString(), context);
        execution.setStatus(ExecutionStatus.RUNNING);

        String taskInput = context.conversation().messages().isEmpty()
                ? ""
                : context.conversation().messages().get(context.conversation().messages().size() - 1).content();

        StringBuilder observationLog = new StringBuilder();
        int maxSteps = agent.maxSteps() > 0 ? agent.maxSteps() : 5;

        for (int step = 1; step <= maxSteps; step++) {
            context.runtime().setCurrentStep(step);
            context.runtime().setCurrentRuntime("AgentRuntime:" + agent.id());

            PlannerDecision decision;
            try {
                decision = planner.decide(agent, taskInput, observationLog.toString());
            } catch (Exception e) {
                execution.setStatus(ExecutionStatus.FAILED);
                execution.setErrorMessage("Planner failed: " + e.getMessage());
                return execution;
            }

            if (decision.type() == PlannerDecision.Type.FINISH) {
                execution.setStatus(ExecutionStatus.SUCCESS);
                execution.setResult(decision.finalAnswer());
                context.conversation().addAssistantMessage(decision.finalAnswer());
                return execution;
            }

            // ACTION: dispatch to ToolRuntime (or recursively to AgentRuntime if the tool id matches an agent)
            JsonNode args;
            try {
                args = MAPPER.readTree(decision.toolArgsJson());
            } catch (Exception e) {
                execution.setStatus(ExecutionStatus.FAILED);
                execution.setErrorMessage("Invalid tool args from planner: " + decision.toolArgsJson());
                return execution;
            }

            context.variables().set("__tool_args__", com.cs.online.context.VariableType.OBJECT, args);
            ToolDefinition toolDefinition = new ToolDefinition(decision.toolId(), "1.0", decision.toolId(), null);
            Execution actionExecution = dispatcher.dispatch(toolDefinition, context);

            String observation = actionExecution.status() == ExecutionStatus.SUCCESS
                    ? "Tool[" + decision.toolId() + "] result: " + describe(actionExecution)
                    : "Tool[" + decision.toolId() + "] failed: " + describe(actionExecution);
            observationLog.append(observation).append("\n");
            context.observation().record(observation);
        }

        execution.setStatus(ExecutionStatus.FAILED);
        execution.setErrorMessage("Agent exceeded maxSteps=" + maxSteps + " without finishing");
        return execution;
    }

    private String describe(Execution execution) {
        if (execution instanceof DefaultExecution de) {
            return de.getResult() != null ? String.valueOf(de.getResult()) : String.valueOf(de.getErrorMessage());
        }
        return execution.status().name();
    }
}
