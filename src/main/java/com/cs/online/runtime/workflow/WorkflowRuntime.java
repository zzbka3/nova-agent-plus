package com.cs.online.runtime.workflow;

import com.cs.online.context.Context;
import com.cs.online.context.VariableType;
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
 * IF 节点支持条件分支（一次只走一条路径），不支持并行 fan-out。
 */
@Component
public class WorkflowRuntime implements Runtime {

    private static final String DEFAULT_BRANCH_KEY = "default";

    private final ExecutorDispatcher dispatcher;
    private final SpelEvaluator spelEvaluator;

    public WorkflowRuntime(@Lazy ExecutorDispatcher dispatcher, SpelEvaluator spelEvaluator) {
        this.dispatcher = dispatcher;
        this.spelEvaluator = spelEvaluator;
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
        Map<String, List<Edge>> edgesByFrom = workflow.edges().stream()
                .collect(Collectors.groupingBy(Edge::from));

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

            List<Edge> outgoing = edgesByFrom.getOrDefault(current.id(), List.of());

            if (current.type() == NodeType.IF) {
                String branchKey;
                try {
                    Object value = spelEvaluator.evaluate(current.config().get("expression").asText(), context);
                    branchKey = String.valueOf(value);
                } catch (Exception e) {
                    execution.setStatus(ExecutionStatus.FAILED);
                    execution.setErrorMessage("IF node " + current.id() + " expression evaluation failed: " + e.getMessage());
                    return execution;
                }

                Edge chosen = outgoing.stream()
                        .filter(edge -> branchKey.equals(edge.branchKey()))
                        .findFirst()
                        .or(() -> outgoing.stream().filter(edge -> DEFAULT_BRANCH_KEY.equals(edge.branchKey())).findFirst())
                        .orElse(null);

                if (chosen == null) {
                    execution.setStatus(ExecutionStatus.FAILED);
                    execution.setErrorMessage("IF node " + current.id() + " has no edge matching branch '" + branchKey + "' and no default edge");
                    return execution;
                }

                context.observation().record("[Workflow:" + workflow.id() + "] node " + current.id() + " branch=" + branchKey + " -> " + chosen.to());
                current = nodesById.get(chosen.to());
                continue;
            }

            Execution nodeExecution = runNode(current, context);
            if (nodeExecution.status() != ExecutionStatus.SUCCESS) {
                execution.setStatus(ExecutionStatus.FAILED);
                execution.setErrorMessage("Node failed: " + current.id());
                return execution;
            }
            lastResult = nodeExecution instanceof DefaultExecution de ? de.getResult() : null;
            context.variables().set("node:" + current.id(), VariableType.OBJECT, lastResult);

            if (outgoing.size() > 1) {
                execution.setStatus(ExecutionStatus.FAILED);
                execution.setErrorMessage("Node " + current.id() + " has multiple outgoing edges but is not an IF node");
                return execution;
            }

            current = outgoing.isEmpty() ? null : nodesById.get(outgoing.get(0).to());
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
                context.variables().set("__tool_args__", VariableType.OBJECT, node.config().get("args"));
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
