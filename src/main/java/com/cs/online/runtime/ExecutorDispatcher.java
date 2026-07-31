package com.cs.online.runtime;

import com.cs.online.context.Context;
import com.cs.online.execution.Execution;
import com.cs.online.resource.Resource;
import com.cs.online.resource.ResourceType;
import com.cs.online.runtime.agent.AgentRuntime;
import com.cs.online.runtime.tool.ToolRuntime;
import com.cs.online.runtime.workflow.WorkflowRuntime;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * 统一调度入口：AgentRuntime -> ExecutorDispatcher -> {WorkflowRuntime, ToolRuntime, AgentRuntime(递归)}。
 * AgentRuntime 依赖本类做分发，本类又需要能分发回 AgentRuntime，因此对 AgentRuntime 使用懒加载打破循环依赖。
 */
@Component
public class ExecutorDispatcher {

    private final ToolRuntime toolRuntime;
    private final WorkflowRuntime workflowRuntime;
    private final AgentRuntime agentRuntime;

    public ExecutorDispatcher(ToolRuntime toolRuntime,
                               WorkflowRuntime workflowRuntime,
                               @Lazy AgentRuntime agentRuntime) {
        this.toolRuntime = toolRuntime;
        this.workflowRuntime = workflowRuntime;
        this.agentRuntime = agentRuntime;
    }

    public Execution dispatch(Resource resource, Context context) {
        ResourceType type = resource.type();
        return switch (type) {
            case TOOL -> toolRuntime.execute(resource, context);
            case WORKFLOW -> workflowRuntime.execute(resource, context);
            case AGENT -> agentRuntime.execute(resource, context);
            default -> throw new IllegalArgumentException("Unsupported resource type for dispatch: " + type);
        };
    }
}
