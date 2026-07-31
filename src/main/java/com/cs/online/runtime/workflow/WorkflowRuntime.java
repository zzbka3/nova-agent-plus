package com.cs.online.runtime.workflow;

import com.cs.online.context.Context;
import com.cs.online.execution.DefaultExecution;
import com.cs.online.execution.Execution;
import com.cs.online.execution.ExecutionStatus;
import com.cs.online.resource.AgentDefinition;
import com.cs.online.resource.Resource;
import com.cs.online.resource.ToolDefinition;
import com.cs.online.resource.WorkflowDefinition;
import com.cs.online.runtime.ExecutorDispatcher;
import com.cs.online.runtime.Runtime;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Workflow 不负责思考，只负责按照 DAG 执行：Node -> Edge -> Next Node -> End。
 */
@Component
public class WorkflowRuntime implements Runtime {

    private final ExecutorDispatcher dispatcher;

    public WorkflowRuntime(@Lazy ExecutorDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public Execution execute(Resource resource, Context context) {
        if (!(resource instanceof WorkflowDefinition workflow)) {
            throw new IllegalArgumentException("WorkflowRuntime can only execute WorkflowDefinition resources");
        }

        DefaultExecution execution = new DefaultExecution(UUID.randomUUID().toString(), context);
        execution.setStatus(ExecutionStatus.RUNNING);

        Map<String, Node> nodesById = workflow.nodes().stream()
                .collect(Collectors.toMap(Node::id, Function.identity()));
        Map<String, String> nextNodeByFrom = workflow.edges().stream()
                .collect(Collectors.toMap(Edge::from, Edge::to));

        Optional<Node> startNode = workflow.nodes().stream().findFirst();
        if (startNode.isEmpty()) {
            execution.setStatus(ExecutionStatus.FAILED);
            execution.setErrorMessage("Workflow has no nodes: " + workflow.id());
            return execution;
        }

        Node current = startNode.get();
        Object lastResult = null;
        int guard = 0;
        while (current != null && guard++ < 1000) {
            context.observation().record("[Workflow:" + workflow.id() + "] entering node " + current.id() + " (" + current.type() + ")");

            if (current.type() == NodeType.END) {
                execution.setStatus(ExecutionStatus.SUCCESS);
                execution.setResult(lastResult);
                return execution;
            }

            Execution nodeExecution = runNode(current, context);
            if (nodeExecution.status() != ExecutionStatus.SUCCESS) {
                execution.setStatus(ExecutionStatus.FAILED);
                execution.setErrorMessage("Node failed: " + current.id());
                return execution;
            }
            lastResult = nodeExecution instanceof DefaultExecution de ? de.getResult() : null;

            String nextId = nextNodeByFrom.get(current.id());
            current = nextId == null ? null : nodesById.get(nextId);
        }

        execution.setStatus(ExecutionStatus.SUCCESS);
        execution.setResult(lastResult);
        return execution;
    }

    private Execution runNode(Node node, Context context) {
        return switch (node.type()) {
            case TOOL -> {
                String toolId = node.config().get("toolId").asText();
                ToolDefinition toolDefinition = new ToolDefinition(toolId, "1.0", toolId, null);
                context.variables().set("__tool_args__", com.cs.online.context.VariableType.OBJECT, node.config().get("args"));
                yield dispatcher.dispatch(toolDefinition, context);
            }
            case AGENT -> {
                String agentId = node.config().get("agentId").asText();
                String input = node.config().has("input") ? node.config().get("input").asText() : "";
                context.conversation().addUserMessage(input);
                AgentDefinition agentDefinition = new AgentDefinition(agentId, "1.0", agentId, null, null, List.of(), 5);
                yield dispatcher.dispatch(agentDefinition, context);
            }
            default -> throw new UnsupportedOperationException("Node type not supported in V1: " + node.type());
        };
    }
}
